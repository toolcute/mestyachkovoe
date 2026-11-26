package com.cutetool.mestyachkovoe.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import com.cutetool.mestyachkovoe.game.Bubble
import com.cutetool.mestyachkovoe.game.CutisEngine
import com.cutetool.mestyachkovoe.game.CutisState
import com.cutetool.mestyachkovoe.game.CutisViewModel
import com.cutetool.mestyachkovoe.game.KotiFace
import com.cutetool.mestyachkovoe.game.RoomType
import com.cutetool.mestyachkovoe.game.buildKoti
import com.cutetool.mestyachkovoe.game.buildRoomBackground
import com.cutetool.mestyachkovoe.game.buildStar
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private object CutisColors {
    val ink = Color(0xFF233019)
    val inkSoft = Color(0x88233019)
    val cream = Color(0xFFF7FFE4)
    val panel = Color(0xFFF1FBDD)
    val lime = Color(0xFFB7EC66)
    val limeDeep = Color(0xFF7CC23A)
    val pink = Color(0xFFFFC3D5)
    val coin = Color(0xFFFFE14D)
    val coinDark = Color(0xFFC99E18)
    val shadow = Color(0x66000000)
    val barHunger = Color(0xFFF0A63C)
    val barSleep = Color(0xFF7C9BE8)
    val barMood = Color(0xFFF27BA5)
    val barHygiene = Color(0xFF5FC6BC)
}

@Composable
fun CutisScreen(onExit: () -> Unit, vm: CutisViewModel = viewModel()) {
    BackHandler { onExit() }
    val state by vm.state.collectAsState()
    val bubble by vm.bubble.collectAsState()

    DisposableEffect(Unit) {
        onDispose { vm.persistNow() }
    }

    val current = state
    if (current == null) {
        Box(Modifier.fillMaxSize().background(Color(0xFF10210F)), contentAlignment = Alignment.Center) {
            Text(
                "CUTIS…",
                color = CutisColors.lime,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
            )
        }
        return
    }

    val pet = current.pets.firstOrNull() ?: return
    val background = remember(current.room) { buildRoomBackground(current.room) }

    Box(Modifier.fillMaxSize().background(Color(0xFF0F1D12))) {
        PixelImage(background, Modifier.fillMaxSize(), ContentScale.Crop)
        Scanlines()
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 12.dp),
        ) {
            CutisTopBar(coins = current.coins, onExit = onExit)
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 14.dp),
            ) {
                Scene(
                    state = current,
                    bubble = bubble,
                    onTap = vm::tapPet,
                )
                GamePanel(
                    state = current,
                    onFeed = vm::feed,
                    onSleep = vm::sleep,
                    onWash = vm::wash,
                    onPet = vm::pet,
                    onRoom = vm::selectRoom,
                    onLockedPet = vm::lockedPet,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Статы падают медленно — заходи раз в 1–1,5 дня. " +
                        "Все статы ≥ 10% = пассивный доход.",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 13.sp,
                    modifier = Modifier.padding(horizontal = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun CutisTopBar(coins: Int, onExit: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PixelSquareButton(text = "‹", onClick = onExit)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "CUTIS",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 3.sp,
                style = TextStyle(
                    shadow = Shadow(Color(0xCC0A1A0C), Offset(0f, 3f), 3f),
                ),
            )
            Text(
                "тамагочи-уголок",
                color = Color(0xFFE8FFD2),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp,
            )
        }
        CoinPill(coins)
    }
}

@Composable
private fun CoinPill(coins: Int) {
    Row(
        Modifier
            .shadow(4.dp, RoundedCornerShape(20.dp))
            .background(CutisColors.cream, RoundedCornerShape(20.dp))
            .border(2.dp, CutisColors.ink, RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(16.dp)) {
            drawCircle(CutisColors.coinDark)
            drawCircle(CutisColors.coin, radius = size.minDimension / 2f - 1.2f)
            drawCircle(Color(0x66FFFFFF), radius = size.minDimension / 5f, center = Offset(size.width * 0.34f, size.height * 0.32f))
        }
        Spacer(Modifier.width(6.dp))
        Text(
            coins.toString(),
            color = CutisColors.ink,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
        )
        Spacer(Modifier.width(3.dp))
        Text(
            "у.е.",
            color = CutisColors.inkSoft,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun Scene(state: CutisState, bubble: Bubble?, onTap: () -> Unit) {
    val pet = state.pets.first()
    val face = when {
        pet.stats.lowest < 35f -> KotiFace.SAD
        pet.stats.average > 68f || state.affection >= 20 -> KotiFace.HAPPY
        else -> KotiFace.NORMAL
    }
    val koti = remember(face) { buildKoti(face) }
    val star = remember { buildStar(19) }
    val bob by rememberInfiniteTransition(label = "bob").animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            tween(1700, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
        ),
        label = "bobY",
    )

    Box(
        Modifier
            .fillMaxWidth()
            .height(296.dp)
            .padding(top = 4.dp),
    ) {
        Text(
            "${state.room.emoji} ${state.room.title}",
            color = Color.White,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 4.dp)
                .shadow(2.dp, RoundedCornerShape(6.dp))
                .background(Color(0x44000000), RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 3.dp),
        )

        FloatingStar(star, Modifier.align(Alignment.TopStart).offset(x = 28.dp, y = 52.dp).size(30.dp), phase = 0f)
        FloatingStar(star, Modifier.align(Alignment.TopEnd).offset(x = (-20).dp, y = 150.dp).size(24.dp), phase = 0.45f)
        FloatingStar(star, Modifier.align(Alignment.TopStart).offset(x = 12.dp, y = 190.dp).size(20.dp), phase = 0.7f)

        Column(
            Modifier.align(Alignment.Center).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.height(58.dp).fillMaxWidth(),
                contentAlignment = Alignment.BottomCenter,
            ) {
                BubbleSlot(bubble)
            }
            PixelImage(
                koti,
                Modifier
                    .size(222.dp)
                    .offset(y = bob.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onTap,
                    ),
                ContentScale.Fit,
            )
            Text(
                if (bubble == null) "тапни Кути • погладить — кнопка ниже" else " ",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

@Composable
private fun FloatingStar(bitmap: ImageBitmap, modifier: Modifier, phase: Float) {
    val transition = rememberInfiniteTransition(label = "star$phase")
    val alpha by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(1400, delayMillis = (phase * 1000).toInt(), easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
        ),
        label = "starAlpha",
    )
    val spin by transition.animateFloat(
        initialValue = -6f + phase * 12f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            tween(2600, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
        ),
        label = "starSpin",
    )
    Canvas(modifier) {
        rotate(spin, pivot = center) {
            drawImage(
                image = bitmap,
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                alpha = alpha,
                filterQuality = FilterQuality.None,
            )
        }
    }
}

@Composable
private fun BubbleSlot(bubble: Bubble?) {
    AnimatedVisibility(
        visible = bubble != null,
        enter = fadeIn(tween(120)) + scaleIn(initialScale = 0.7f),
        exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.85f),
    ) {
        SpeechBubble(bubble?.text.orEmpty())
    }
}

@Composable
private fun SpeechBubble(text: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .shadow(6.dp, RoundedCornerShape(14.dp))
                .background(CutisColors.cream, RoundedCornerShape(14.dp))
                .border(2.dp, CutisColors.ink, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 9.dp),
        ) {
            Text(
                text,
                color = CutisColors.ink,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                textAlign = TextAlign.Center,
            )
        }
        Canvas(Modifier.size(width = 16.dp, height = 9.dp)) {
            val tail = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width / 2f, size.height)
                close()
            }
            drawPath(tail, CutisColors.cream)
            drawLine(
                CutisColors.ink,
                Offset(0.5f, 0f),
                Offset(size.width / 2f, size.height),
                strokeWidth = 2.dp.toPx(),
            )
            drawLine(
                CutisColors.ink,
                Offset(size.width - 0.5f, 0f),
                Offset(size.width / 2f, size.height),
                strokeWidth = 2.dp.toPx(),
            )
        }
    }
}

@Composable
private fun GamePanel(
    state: CutisState,
    onFeed: () -> Unit,
    onSleep: () -> Unit,
    onWash: () -> Unit,
    onPet: () -> Unit,
    onRoom: (RoomType) -> Unit,
    onLockedPet: (String, Int) -> Unit,
) {
    val pet = state.pets.first()
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(10.dp, shape)
            .background(CutisColors.panel, shape)
            .border(3.dp, CutisColors.ink, shape)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "СОСТОЯНИЕ",
                color = CutisColors.ink,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.weight(1f))
            Text("♥ ${state.affection}", color = Color(0xFFE05A87), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        StatRow("🍙", "Сытость", pet.stats.hunger, CutisColors.barHunger)
        StatRow("💤", "Сон", pet.stats.sleep, CutisColors.barSleep)
        StatRow("🎈", "Настроение", pet.stats.mood, CutisColors.barMood)
        StatRow("🧼", "Чистота", pet.stats.hygiene, CutisColors.barHygiene)

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PixelButton("🍙 Корм · 1", Modifier.weight(1f), accent = CutisColors.lime, onClick = onFeed)
            PixelButton("💤 Спать", Modifier.weight(1f), accent = CutisColors.cream, onClick = onSleep)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PixelButton("🛁 Мытьё · 1", Modifier.weight(1f), accent = CutisColors.cream, onClick = onWash)
            PixelButton("💗 Гладить", Modifier.weight(1f), accent = CutisColors.pink, onClick = onPet)
        }

        Spacer(Modifier.height(12.dp))
        DashedDivider()
        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "КОМНАТЫ",
                color = CutisColors.ink,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.weight(1f))
            val earning = CutisEngine.isEarningIncome(pet)
            Row(
                Modifier
                    .background(
                        if (earning) Color(0xFFCFEFA0) else Color(0xFFE2E7D8),
                        RoundedCornerShape(10.dp),
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("+1 у.е./ч", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = CutisColors.ink)
                Spacer(Modifier.width(4.dp))
                Canvas(Modifier.size(6.dp)) {
                    drawCircle(if (earning) Color(0xFF3F9E22) else Color(0xFF9AA48E))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(end = 4.dp)) {
            items(RoomType.entries.toList()) { room ->
                RoomChip(room, room == state.room) { onRoom(room) }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            "ДОМ",
            color = CutisColors.ink,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PetChip(
                name = "Кути",
                subtitle = "дома",
                accent = CutisColors.lime,
                locked = false,
                modifier = Modifier.weight(1f),
                onClick = onPet,
            )
            PetChip(
                name = "Мику!",
                subtitle = "500 у.е.",
                accent = CutisColors.cream,
                locked = true,
                modifier = Modifier.weight(1f),
                onClick = { onLockedPet("Мику!", 500) },
            )
            PetChip(
                name = "Зубы",
                subtitle = "800 у.е.",
                accent = CutisColors.cream,
                locked = true,
                modifier = Modifier.weight(1f),
                onClick = { onLockedPet("Зубы", 800) },
            )
        }
    }
}

@Composable
private fun StatRow(emoji: String, label: String, value: Float, color: Color) {
    Row(
        Modifier.fillMaxWidth().height(19.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, fontSize = 11.sp, modifier = Modifier.width(18.dp))
        Text(
            label,
            color = CutisColors.ink,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(78.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Box(
            Modifier
                .weight(1f)
                .height(11.dp)
                .background(Color(0xFFDCE7CB), RoundedCornerShape(3.dp))
                .border(1.dp, CutisColors.inkSoft, RoundedCornerShape(3.dp))
                .padding(1.dp),
        ) {
            Box(
                Modifier
                    .fillMaxWidth((value / 100f).coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(color, RoundedCornerShape(2.dp)),
            )
        }
        Text(
            "${value.roundToInt()}%",
            color = CutisColors.ink,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.End,
            modifier = Modifier.width(34.dp),
        )
    }
}

@Composable
private fun PixelButton(
    text: String,
    modifier: Modifier = Modifier,
    accent: Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier
            .offset(y = if (pressed) 2.dp else 0.dp)
            .shadow(if (pressed) 1.dp else 4.dp, shape)
            .background(if (enabled) accent else Color(0xFFD5DCC8), shape)
            .border(2.dp, if (enabled) CutisColors.ink else CutisColors.inkSoft, shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .heightIn(min = 40.dp)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = CutisColors.ink,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PixelSquareButton(text: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        Modifier
            .size(40.dp)
            .shadow(4.dp, shape)
            .background(CutisColors.cream, shape)
            .border(2.dp, CutisColors.ink, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = CutisColors.ink,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.offset(y = (-2).dp),
        )
    }
}

@Composable
private fun RoomChip(room: RoomType, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        Modifier
            .background(if (selected) CutisColors.lime else CutisColors.cream, shape)
            .border(if (selected) 2.dp else 1.dp, CutisColors.ink.copy(alpha = if (selected) 1f else 0.4f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(room.emoji, fontSize = 11.sp)
        Spacer(Modifier.width(4.dp))
        Text(
            room.title,
            color = CutisColors.ink,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun PetChip(
    name: String,
    subtitle: String,
    accent: Color,
    locked: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier
            .background(accent, shape)
            .border(2.dp, CutisColors.ink.copy(alpha = if (locked) 0.35f else 1f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(if (locked) "🔒" else "🐱", fontSize = 13.sp)
        Spacer(Modifier.width(5.dp))
        Column {
            Text(
                name,
                color = CutisColors.ink,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
            )
            Text(
                subtitle,
                color = CutisColors.inkSoft,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun DashedDivider() {
    Canvas(Modifier.fillMaxWidth().height(2.dp)) {
        val dash = 4.dp.toPx()
        var x = 0f
        while (x < size.width) {
            drawLine(CutisColors.inkSoft, Offset(x, 1.dp.toPx()), Offset((x + dash).coerceAtMost(size.width), 1.dp.toPx()), 1.dp.toPx())
            x += dash * 2
        }
    }
}

@Composable
private fun Scanlines() {
    Canvas(Modifier.fillMaxSize()) {
        val step = 3.dp.toPx()
        var y = 0f
        while (y < size.height) {
            drawLine(Color(0x12000000), Offset(0f, y), Offset(size.width, y), 1f)
            y += step
        }
        drawRect(
            Brush.radialGradient(
                colors = listOf(Color.Transparent, Color(0x55031206)),
                center = center,
                radius = max(size.width, size.height) * 0.78f,
            )
        )
    }
}

@Composable
private fun PixelImage(bitmap: ImageBitmap, modifier: Modifier, contentScale: ContentScale) {
    Canvas(modifier) {
        if (bitmap.width == 0 || bitmap.height == 0) return@Canvas
        val scale = when (contentScale) {
            ContentScale.Crop -> max(size.width / bitmap.width, size.height / bitmap.height)
            ContentScale.Fit -> min(size.width / bitmap.width, size.height / bitmap.height)
            else -> 1f
        }
        val w = (bitmap.width * scale).roundToInt()
        val h = (bitmap.height * scale).roundToInt()
        drawImage(
            image = bitmap,
            dstOffset = IntOffset((size.width - w).roundToInt() / 2, (size.height - h).roundToInt() / 2),
            dstSize = IntSize(w, h),
            filterQuality = FilterQuality.None,
        )
    }
}
