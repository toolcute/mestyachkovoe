package com.cutetool.mestyachkovoe.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.cutetool.mestyachkovoe.data.CuteIdRepository
import com.cutetool.mestyachkovoe.data.NewsItem
import com.cutetool.mestyachkovoe.data.NewsRepository
import com.cutetool.mestyachkovoe.data.SavedPost
import com.cutetool.mestyachkovoe.data.SettingsRepository
import com.cutetool.mestyachkovoe.data.Sources
import com.cutetool.mestyachkovoe.data.UserSettings
import com.cutetool.mestyachkovoe.ui.theme.ExpressiveShapes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    settings: UserSettings,
    settingsRepo: SettingsRepository,
    onClose: () -> Unit,
    onOpenNews: (NewsItem) -> Unit,
    onCheckCid: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var miniFeedUrl by remember { mutableStateOf<String?>(null) }
    var showFullCid by remember { mutableStateOf(false) }
    val savedTitles = remember(settings.savedPosts) {
        settings.savedPosts.map { it.key }.toSet()
    }

    LaunchedEffect(Unit) {
        delay(1000)
        showFullCid = true
    }

    fun syncCid() {
        scope.launch { CuteIdRepository.sendSync(settingsRepo.snapshot()) }
    }

    val currentMiniFeed = miniFeedUrl
    if (currentMiniFeed != null) {
        MiniFeedScreen(
            url = currentMiniFeed,
            settings = settings,
            settingsRepo = settingsRepo,
            onBack = { miniFeedUrl = null },
            onOpenNews = onOpenNews,
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { AnimatedCidTitle(showFullCid) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                CidAccountSection(
                    settings = settings,
                    settingsRepo = settingsRepo,
                    onSync = { syncCid() },
                    onCheckCid = onCheckCid
                )
            }

            item { DividerLine() }

            item {
                SectionTitle(
                    title = "Любимые статьи",
                    subtitle = if (settings.savedPosts.isEmpty()) {
                        "Нажимайте сердечко на карточке новости"
                    } else {
                        "Сохранено: ${settings.savedPosts.size}"
                    }
                )
            }

            if (settings.savedPosts.isEmpty()) {
                item { EmptyHint("Пока нет любимых статей.") }
            } else {
                items(settings.savedPosts, key = { it.key }) { post ->
                    SavedPostCard(
                        post = post,
                        onOpen = if (post.link.isNotBlank()) {
                            { onOpenNews(post.toNewsItem()) }
                        } else null,
                        onRemove = {
                            scope.launch {
                                settingsRepo.removeSavedPost(post)
                                syncCid()
                            }
                        }
                    )
                }
            }

            item { DividerLine() }

            item {
                SectionTitle(
                    title = "Любимые источники",
                    subtitle = if (settings.savedSources.isEmpty()) {
                        "Кнопка «Источник в любимое!» в новости"
                    } else {
                        "Сохранено: ${settings.savedSources.size}"
                    }
                )
            }

            if (settings.savedSources.isEmpty()) {
                item { EmptyHint("Пока нет любимых источников.") }
            } else {
                items(settings.savedSources.sortedBy { favoriteSourceName(it, settings) }, key = { it }) { url ->
                    SavedSourceCard(
                        name = favoriteSourceName(url, settings),
                        url = url,
                        onOpen = { miniFeedUrl = url },
                        onRemove = {
                            scope.launch {
                                settingsRepo.toggleSavedSource(url)
                                syncCid()
                            }
                        }
                    )
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun CidAccountSection(
    settings: UserSettings,
    settingsRepo: SettingsRepository,
    onSync: () -> Unit,
    onCheckCid: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var nameDialogOpen by remember { mutableStateOf(false) }
    val nickname = settings.cidNickname.orEmpty()
    val hasValidName = CuteIdRepository.isValidNickname(nickname)
    val pending = !settings.cidLinked && !settings.cidSession.isNullOrBlank()
    val displayName = nickname.ifBlank { "CuteID" }
    val avatarLetter = (displayName.firstOrNull() ?: 'C').uppercaseChar()
    val cover = Brush.linearGradient(
        listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.secondary
        )
    )

    fun checkInTelegram() {
        if (!settings.cidLinked && settings.cidSession.isNullOrBlank()) {
            if (!hasValidName) {
                nameDialogOpen = true
                return
            }
            val session = CuteIdRepository.newSessionToken()
            scope.launch {
                settingsRepo.beginCidAuth(nickname, session)
                onCheckCid()
                val username = CuteIdRepository.botUsername()
                openTelegram(
                    context,
                    CuteIdRepository.authUrl(username, session, nickname)
                )
            }
        } else {
            onCheckCid()
            scope.launch {
                val username = CuteIdRepository.botUsername()
                val session = settings.cidSession
                val nick = settings.cidNickname
                val url = if (!settings.cidLinked && session != null && nick != null) {
                    CuteIdRepository.authUrl(username, session, nick)
                } else {
                    "https://t.me/$username"
                }
                openTelegram(context, url)
            }
        }
    }

    Card(
        shape = RoundedCornerShape(34.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(34.dp))) {
            Box(Modifier.fillMaxWidth().height(170.dp).background(cover)) {
                Box(
                    Modifier
                        .width(210.dp)
                        .height(86.dp)
                        .offset(x = (-42).dp, y = 18.dp)
                        .rotate(-16f)
                        .background(Color.White.copy(alpha = 0.14f), CircleShape)
                )
                Box(
                    Modifier
                        .size(108.dp)
                        .offset(x = 244.dp, y = 16.dp)
                        .rotate(12f)
                        .background(Color.White.copy(alpha = 0.10f), ExpressiveShapes.arch)
                )
                Box(
                    Modifier
                        .width(124.dp)
                        .height(54.dp)
                        .offset(x = 168.dp, y = 112.dp)
                        .rotate(-8f)
                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(27.dp))
                )
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 18.dp, top = 118.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Surface(
                        shape = RoundedCornerShape(30.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        border = BorderStroke(4.dp, MaterialTheme.colorScheme.surfaceContainerLow),
                        modifier = Modifier.size(88.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                avatarLetter.toString(),
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            displayName,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CidStatusPill(linked = settings.cidLinked, pending = pending)
                            if (settings.cidLinked) {
                                settings.cidTgId?.let {
                                    Text(
                                        "ID $it",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Text(
                    when {
                        settings.cidLinked ->
                            "Любимое синхронизируется между устройствами. Галочка слева открывает смену имени."
                        pending ->
                            "Откройте Telegram и нажмите Start. Сессия появится в users.json автоматически."
                        else ->
                            "Необязательный аккаунт CuteID. Галочка слева — имя, кнопка — вход через Telegram."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalIconButton(
                        onClick = { nameDialogOpen = true },
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Изменить имя CuteID",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Button(
                        onClick = { checkInTelegram() },
                        shape = ExpressiveShapes.button,
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = null,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Проверить в Telegram")
                    }
                }

                if (pending) {
                    Surface(
                        shape = ExpressiveShapes.tile,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ExpressiveLoadingIndicator(size = 18.dp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Проверяем сессию CuteID…",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    "Каждые 2 секунды и при возврате в приложение",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (nameDialogOpen) {
        NameEditDialog(
            currentName = nickname,
            onDismiss = { nameDialogOpen = false },
            onSave = { clean ->
                scope.launch {
                    settingsRepo.setCidNickname(clean)
                    if (settings.cidLinked) onSync()
                }
                nameDialogOpen = false
            }
        )
    }
}

@Composable
private fun NameEditDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var value by remember(currentName) { mutableStateOf(currentName) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = ExpressiveShapes.hero,
        icon = {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        },
        title = { Text("Имя CuteID", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "4–9 английских букв. Это имя увидят только вы и CuteID.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = {
                        value = it.filter { ch -> ch in 'a'..'z' || ch in 'A'..'Z' }
                        error = null
                    },
                    label = { Text("Имя") },
                    placeholder = { Text("Например: Cute") },
                    singleLine = true,
                    isError = error != null || (value.isNotBlank() && !CuteIdRepository.isValidNickname(value)),
                    supportingText = {
                        Text(error ?: "Например: Cute, Cutie, Mestechko")
                    },
                    shape = RoundedCornerShape(18.dp),
                    colors = cidFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clean = value.trim()
                    if (CuteIdRepository.isValidNickname(clean)) {
                        onSave(clean)
                    } else {
                        error = "Нужно 4–9 английских букв"
                    }
                },
                shape = ExpressiveShapes.button
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
private fun CidStatusPill(linked: Boolean, pending: Boolean) {
    val text = when {
        linked -> "Подключено"
        pending -> "Ожидание"
        else -> "Не подключено"
    }
    val color = when {
        linked -> MaterialTheme.colorScheme.primary
        pending -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(shape = CircleShape, color = color.copy(alpha = 0.16f)) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun cidFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    cursorColor = MaterialTheme.colorScheme.primary,
)

@Composable
private fun AnimatedCidTitle(showFull: Boolean) {
    val style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Мой ", style = style)
        Text("C", style = style)
        AnimatedVisibility(
            visible = showFull,
            enter = expandHorizontally(
                expandFrom = Alignment.Start,
                animationSpec = tween(520, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(320, delayMillis = 90)),
            exit = shrinkHorizontally(
                shrinkTowards = Alignment.Start,
                animationSpec = tween(200, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(160))
        ) {
            Text("ute", style = style)
        }
        Text("ID", style = style)
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyHint(text: String) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Text(
            text,
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DividerLine() {
    androidx.compose.material3.HorizontalDivider()
}

@Composable
private fun SavedPostCard(
    post: SavedPost,
    onOpen: (() -> Unit)?,
    onRemove: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onOpen != null) Modifier.clickable(onClick = onOpen) else Modifier),
        shape = ExpressiveShapes.card
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!post.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = post.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    post.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (post.source.isNotBlank()) {
                        Text(
                            post.source,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (post.published > 0L) {
                        Text(
                            formatTime(post.published),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = "Убрать из любимого",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SavedSourceCard(
    name: String,
    url: String,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = ExpressiveShapes.card
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                Icons.Default.OpenInNew,
                contentDescription = "Открыть мини-ленту",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Убрать источник")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MiniFeedScreen(
    url: String,
    settings: UserSettings,
    settingsRepo: SettingsRepository,
    onBack: () -> Unit,
    onOpenNews: (NewsItem) -> Unit,
) {
    val repo = remember { NewsRepository() }
    val scope = rememberCoroutineScope()
    val savedTitles = remember(settings.savedPosts) {
        settings.savedPosts.map { it.key }.toSet()
    }
    var items by remember(url) { mutableStateOf<List<NewsItem>>(emptyList()) }
    var loading by remember(url) { mutableStateOf(true) }
    var error by remember(url) { mutableStateOf<String?>(null) }

    LaunchedEffect(url) {
        loading = true
        error = null
        try {
            items = repo.loadSource(url, settings.customSources)
        } catch (e: Exception) {
            error = e.message ?: "Не удалось загрузить источник"
        }
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            favoriteSourceName(url, settings),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "Мини-лента источника · без фильтрации",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                loading -> ExpressiveLoadingIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    size = 56.dp
                )
                error != null -> Text(
                    error ?: "",
                    Modifier.align(Alignment.Center).padding(20.dp),
                    color = MaterialTheme.colorScheme.error
                )
                items.isEmpty() -> Text(
                    "В этом источнике пока нет постов.",
                    Modifier.align(Alignment.Center).padding(20.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                else -> NewsList(
                    items = items,
                    savedTitles = savedTitles,
                    onItemClick = onOpenNews,
                    onToggleLike = { item ->
                        scope.launch {
                            settingsRepo.toggleSavedPost(item)
                            CuteIdRepository.sendSync(settingsRepo.snapshot())
                        }
                    }
                )
            }
        }
    }
}

internal fun favoriteSourceName(url: String, settings: UserSettings): String {
    (Sources.all + settings.customSources).firstOrNull { it.url == url }?.let { return it.name }
    return runCatching {
        URL(url).host.removePrefix("www.").ifBlank { url }
    }.getOrDefault(url)
}

private fun SavedPost.toNewsItem(): NewsItem = NewsItem(
    id = link.ifBlank { "saved:${title}" },
    title = title,
    link = link,
    imageUrl = imageUrl,
    source = source.ifBlank { "CuteID" },
    published = published,
    region = region,
    feedUrl = feedUrl,
)

private fun openTelegram(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
        .onFailure { openLink(context, url) }
}
