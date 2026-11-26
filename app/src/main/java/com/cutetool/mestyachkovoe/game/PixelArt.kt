package com.cutetool.mestyachkovoe.game

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

internal fun rgb(r: Int, g: Int, b: Int, a: Int = 255): Int =
    (a shl 24) or (r shl 16) or (g shl 8) or b

/** Микро-канвас: рисуем в «низком разрешении», потом растягиваем с FilterQuality.None. */
internal class PixelCanvas(val width: Int, val height: Int) {
    private val buffer = IntArray(width * height)

    fun set(x: Int, y: Int, color: Int) {
        if (x in 0 until width && y in 0 until height) {
            buffer[y * width + x] = color
        }
    }

    fun rect(x0: Int, y0: Int, x1: Int, y1: Int, color: Int) {
        for (y in y0..y1) for (x in x0..x1) set(x, y, color)
    }

    fun ellipse(cx: Float, cy: Float, rx: Float, ry: Float, color: Int) {
        if (rx <= 0f || ry <= 0f) return
        val minX = (cx - rx).toInt() - 1
        val maxX = (cx + rx).toInt() + 2
        val minY = (cy - ry).toInt() - 1
        val maxY = (cy + ry).toInt() + 2
        for (y in minY..maxY) for (x in minX..maxX) {
            val dx = (x + 0.5f - cx) / rx
            val dy = (y + 0.5f - cy) / ry
            if (dx * dx + dy * dy <= 1f) set(x, y, color)
        }
    }

    fun line(x0: Float, y0: Float, x1: Float, y1: Float, color: Int, thickness: Int = 1) {
        val steps = (max(abs(x1 - x0), abs(y1 - y0)) * 2f).toInt().coerceAtLeast(1)
        val t = (thickness - 1) / 2
        for (i in 0..steps) {
            val p = i / steps.toFloat()
            val x = (x0 + (x1 - x0) * p).toInt()
            val y = (y0 + (y1 - y0) * p).toInt()
            rect(x - t, y - t, x + t, y + t, color)
        }
    }

    fun poly(points: List<Pair<Float, Float>>, color: Int) {
        if (points.size < 3) return
        val minX = points.minOf { it.first }.toInt() - 1
        val maxX = points.maxOf { it.first }.toInt() + 2
        val minY = points.minOf { it.second }.toInt() - 1
        val maxY = points.maxOf { it.second }.toInt() + 2
        for (y in minY..maxY) for (x in minX..maxX) {
            if (inside(x + 0.5f, y + 0.5f, points)) set(x, y, color)
        }
    }

    fun ring(cx: Float, cy: Float, radius: Float, thickness: Float, color: Int, yLimit: Int) {
        val outer = radius + thickness / 2f
        for (y in (cy - outer).toInt()..(cy + outer).toInt()) {
            if (y >= yLimit) continue
            for (x in (cx - outer).toInt()..(cx + outer).toInt()) {
                val dx = x + 0.5f - cx
                val dy = y + 0.5f - cy
                val d = sqrt(dx * dx + dy * dy)
                if (d in (radius - thickness / 2f)..(radius + thickness / 2f)) set(x, y, color)
            }
        }
    }

    private fun inside(x: Float, y: Float, pts: List<Pair<Float, Float>>): Boolean {
        var result = false
        var j = pts.lastIndex
        for (i in pts.indices) {
            val xi = pts[i].first
            val yi = pts[i].second
            val xj = pts[j].first
            val yj = pts[j].second
            if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) {
                result = !result
            }
            j = i
        }
        return result
    }

    fun image(): ImageBitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bmp.setPixels(buffer, 0, width, 0, 0, width, height)
        return bmp.asImageBitmap()
    }
}

enum class KotiFace { NORMAL, HAPPY, SAD }

private fun pts(vararg values: Float): List<Pair<Float, Float>> =
    values.toList().chunked(2).map { it[0] to it[1] }

private object SpriteColors {
    val outline = rgb(45, 82, 27)
    val fur = rgb(148, 217, 68)
    val furDark = rgb(113, 184, 48)
    val furLight = rgb(190, 240, 112)
    val ink = rgb(24, 30, 18)
    val white = rgb(255, 255, 255, 230)
    val blush = rgb(229, 155, 143, 200)
    val mouth = rgb(64, 33, 33)
    val pink = rgb(219, 121, 129)
}

/** Кути — космо-кот: зелёная голова, листья-уши, усики-антенны, звёзды. */
internal fun buildKoti(face: KotiFace): ImageBitmap {
    val c = PixelCanvas(72, 72)
    val o = SpriteColors.outline
    val fur = SpriteColors.fur
    val dark = SpriteColors.furDark
    val light = SpriteColors.furLight

    // ушки-листья
    c.poly(pts(12f, 31f, 21f, 5f, 34f, 26f), o)
    c.poly(pts(15.5f, 29f, 21.5f, 10f, 30.5f, 25f), fur)
    c.poly(pts(60f, 31f, 51f, 5f, 38f, 26f), o)
    c.poly(pts(56.5f, 29f, 50.5f, 10f, 41.5f, 25f), fur)

    // антенны
    c.line(33f, 18f, 29f, 6f, o, 2)
    c.line(40f, 18f, 46f, 4f, o, 2)
    c.ellipse(28f, 5f, 3.6f, 3.6f, o)
    c.ellipse(28f, 5f, 2.6f, 2.6f, fur)
    c.ellipse(47f, 3.5f, 3.8f, 3.8f, o)
    c.ellipse(47f, 3.5f, 2.8f, 2.8f, fur)
    c.ellipse(26.5f, 3.8f, 1f, 1f, light)
    c.ellipse(45.5f, 2.3f, 1f, 1f, light)

    // тельце и лапки
    c.ellipse(36f, 58f, 13.5f, 11f, o)
    c.ellipse(36f, 58f, 12.2f, 9.8f, fur)
    c.ellipse(29f, 67f, 5f, 4f, o)
    c.ellipse(29f, 67f, 4f, 3.2f, dark)
    c.ellipse(43f, 67f, 5f, 4f, o)
    c.ellipse(43f, 67f, 4f, 3.2f, dark)
    c.ellipse(23f, 56f, 5f, 4.5f, o)
    c.ellipse(23f, 56f, 4.2f, 3.6f, fur)
    c.ellipse(49f, 56f, 5f, 4.5f, o)
    c.ellipse(49f, 56f, 4.2f, 3.6f, fur)
    c.ellipse(36f, 57f, 5f, 4f, light)

    // голова
    c.ellipse(36f, 36f, 23f, 20f, o)
    c.ellipse(36f, 36f, 22f, 19f, fur)
    c.ellipse(25f, 25f, 7.5f, 4.5f, light)

    // бровки/глазки
    when (face) {
        KotiFace.HAPPY -> {
            c.line(24f, 39f, 28f, 34.5f, SpriteColors.ink, 2)
            c.line(28f, 34.5f, 32f, 39f, SpriteColors.ink, 2)
            c.line(40f, 39f, 44f, 34.5f, SpriteColors.ink, 2)
            c.line(44f, 34.5f, 48f, 39f, SpriteColors.ink, 2)
        }
        KotiFace.SAD -> {
            c.ellipse(28f, 38f, 4.2f, 5.2f, SpriteColors.ink)
            c.ellipse(44f, 38f, 4.2f, 5.2f, SpriteColors.ink)
            c.ellipse(26.5f, 35.5f, 1.3f, 1.3f, SpriteColors.white)
            c.ellipse(42.5f, 35.5f, 1.3f, 1.3f, SpriteColors.white)
            c.line(23f, 31f, 31f, 28f, o, 2)
            c.line(49f, 31f, 41f, 28f, o, 2)
        }
        KotiFace.NORMAL -> {
            c.ellipse(28f, 37.5f, 4.5f, 5.6f, SpriteColors.ink)
            c.ellipse(44f, 37.5f, 4.5f, 5.6f, SpriteColors.ink)
            c.ellipse(26.4f, 34.6f, 1.4f, 1.4f, SpriteColors.white)
            c.ellipse(42.4f, 34.6f, 1.4f, 1.4f, SpriteColors.white)
        }
    }

    // ротик
    when (face) {
        KotiFace.HAPPY -> {
            c.ellipse(36f, 48f, 5.2f, 3.8f, SpriteColors.ink)
            c.ellipse(36f, 49.6f, 2.6f, 1.6f, SpriteColors.pink)
        }
        KotiFace.SAD -> {
            c.line(31f, 50f, 36f, 46.5f, SpriteColors.ink, 2)
            c.line(36f, 46.5f, 41f, 50f, SpriteColors.ink, 2)
        }
        KotiFace.NORMAL -> {
            c.line(30f, 46f, 33f, 49f, SpriteColors.ink, 2)
            c.line(33f, 49f, 36f, 46f, SpriteColors.ink, 2)
            c.line(36f, 46f, 39f, 49f, SpriteColors.ink, 2)
            c.line(39f, 49f, 42f, 46f, SpriteColors.ink, 2)
        }
    }

    // румянец
    c.ellipse(19f, 46f, 4.5f, 2.6f, SpriteColors.blush)
    c.ellipse(53f, 46f, 4.5f, 2.6f, SpriteColors.blush)

    // усики
    val whisker = rgb(88, 146, 40)
    listOf(38f, 42f, 46f).forEachIndexed { i, y ->
        c.line(7f, y - 1f + i, 16f, y, whisker, 1)
        c.line(65f, y - 1f + i, 56f, y, whisker, 1)
    }

    return c.image()
}

/** Жёлтая пятиконечная звезда для «космической» обвязки. */
internal fun buildStar(size: Int = 19): ImageBitmap {
    val c = PixelCanvas(size, size)
    val cx = size / 2f
    val cy = size / 2f
    val outer = size * 0.47f
    val inner = size * 0.21f
    val points = (0 until 10).map { i ->
        val r = if (i % 2 == 0) outer else inner
        val a = -PI / 2 + i * PI / 5
        Pair(cx + (cos(a) * r).toFloat(), cy + (sin(a) * r).toFloat())
    }
    c.poly(points, rgb(196, 152, 24))
    val innerPoints = points.map { p ->
        Pair(cx + (p.first - cx) * 0.82f, cy + (p.second - cy) * 0.82f)
    }
    c.poly(innerPoints, rgb(250, 236, 74))
    c.rect((size * 0.34f).toInt(), (size * 0.28f).toInt(), (size * 0.40f).toInt(), (size * 0.34f).toInt(), rgb(255, 255, 210))
    return c.image()
}

private data class RoomPalette(
    val skyTop: Int,
    val skyBottom: Int,
    val fieldTop: Int,
    val fieldBottom: Int,
    val night: Boolean = false,
    val rainbow: Boolean = false,
    val cloud: Int,
    val cloudShade: Int,
)

private val palettes: Map<RoomType, RoomPalette> = mapOf(
    RoomType.LIVING to RoomPalette(
        skyTop = rgb(36, 108, 210),
        skyBottom = rgb(189, 228, 250),
        fieldTop = rgb(132, 218, 62),
        fieldBottom = rgb(44, 118, 28),
        rainbow = true,
        cloud = rgb(240, 249, 255),
        cloudShade = rgb(198, 219, 243),
    ),
    RoomType.BEDROOM to RoomPalette(
        skyTop = rgb(22, 24, 62),
        skyBottom = rgb(74, 88, 148),
        fieldTop = rgb(38, 76, 48),
        fieldBottom = rgb(14, 32, 22),
        night = true,
        cloud = rgb(150, 162, 208),
        cloudShade = rgb(104, 118, 168),
    ),
    RoomType.PLAYROOM to RoomPalette(
        skyTop = rgb(92, 58, 150),
        skyBottom = rgb(255, 198, 140),
        fieldTop = rgb(104, 186, 66),
        fieldBottom = rgb(38, 92, 36),
        rainbow = true,
        cloud = rgb(255, 236, 220),
        cloudShade = rgb(232, 180, 156),
    ),
    RoomType.KITCHEN to RoomPalette(
        skyTop = rgb(118, 206, 238),
        skyBottom = rgb(232, 251, 255),
        fieldTop = rgb(182, 228, 124),
        fieldBottom = rgb(100, 166, 62),
        cloud = rgb(255, 255, 255),
        cloudShade = rgb(204, 228, 240),
    ),
    RoomType.BATHROOM to RoomPalette(
        skyTop = rgb(243, 178, 220),
        skyBottom = rgb(255, 244, 250),
        fieldTop = rgb(160, 232, 214),
        fieldBottom = rgb(76, 168, 152),
        cloud = rgb(255, 252, 255),
        cloudShade = rgb(232, 208, 226),
    ),
)

private const val VW = 108
private const val VH = 190
private const val HORIZON = 116

internal fun buildRoomBackground(room: RoomType): ImageBitmap {
    val palette = palettes[room] ?: palettes.getValue(RoomType.LIVING)
    val c = PixelCanvas(VW, VH)
    val rnd = Random(room.ordinal * 977 + 13)

    verticalGradient(c, 0, HORIZON, palette.skyTop, palette.skyBottom, 7)

    if (palette.night) {
        repeat(70) {
            val x = rnd.nextInt(0, VW)
            val y = rnd.nextInt(0, HORIZON - 14)
            c.set(x, y, if (rnd.nextBoolean()) rgb(255, 255, 240) else rgb(190, 205, 250))
        }
        // луна
        c.ellipse(84f, 26f, 10f, 10f, rgb(240, 248, 250, 40))
        c.ellipse(84f, 26f, 8f, 8f, rgb(244, 250, 255))
        c.ellipse(88.5f, 23f, 7f, 7f, palette.skyTop)
        c.set(80, 24, rgb(210, 222, 240))
        c.set(82, 30, rgb(210, 222, 240))
    } else {
        c.ellipse(84f, 27f, 12f, 12f, rgb(255, 250, 190, 70))
        c.ellipse(84f, 27f, 9f, 9f, rgb(255, 246, 168))
        c.ellipse(81f, 24f, 4f, 4f, rgb(255, 253, 220))
    }

    if (palette.rainbow) {
        val colors = listOf(
            rgb(226, 74, 66),
            rgb(244, 140, 58),
            rgb(247, 216, 92),
            rgb(120, 204, 86),
            rgb(84, 162, 234),
            rgb(112, 110, 208),
            rgb(172, 108, 210),
        )
        var radius = 68f
        colors.forEach { color ->
            c.ring(46f, HORIZON + 18f, radius, 2.4f, color, HORIZON)
            radius -= 2.4f
        }
    }

    cloud(c, 20f, 26f, 1f, palette.cloud, palette.cloudShade)
    cloud(c, 78f, 17f, 0.85f, palette.cloud, palette.cloudShade)
    cloud(c, 52f, 44f, 0.7f, palette.cloud, palette.cloudShade)
    cloud(c, 100f, 52f, 0.55f, palette.cloud, palette.cloudShade)
    cloud(c, 6f, 58f, 0.6f, palette.cloud, palette.cloudShade)
    cloud(c, 66f, 74f, 0.45f, palette.cloud, palette.cloudShade)

    // линия горизонта
    c.rect(0, HORIZON, VW - 1, HORIZON + 1, palette.fieldTop)
    verticalGradient(c, HORIZON + 2, VH, palette.fieldTop, palette.fieldBottom, 6)

    // травинки и кустики
    repeat(26) {
        val x = rnd.nextInt(0, VW)
        val y = rnd.nextInt(HORIZON + 6, VH)
        val rx = rnd.nextInt(3, 9).toFloat()
        val ry = rnd.nextInt(1, 3).toFloat()
        c.ellipse(x.toFloat(), y.toFloat(), rx, ry, rgb(28, 84, 30, 120))
    }
    repeat(360) {
        val x = rnd.nextInt(0, VW)
        val y = rnd.nextInt(HORIZON + 4, VH)
        val lightSpeck = rnd.nextBoolean()
        c.set(x, y, if (lightSpeck) rgb(196, 248, 118) else rgb(52, 128, 40))
    }
    // дорожка-«шов» как на референсе
    var tx = 12
    for (y in HORIZON + 20 until VH) {
        tx += if (rnd.nextBoolean()) 1 else 0
        c.set(tx, y, rgb(36, 96, 34, 160))
        if (rnd.nextBoolean()) c.set(tx + 1, y, rgb(44, 110, 38, 140))
    }

    return c.image()
}

private fun cloud(c: PixelCanvas, x: Float, y: Float, s: Float, main: Int, shade: Int) {
    c.ellipse(x, y, 11f * s, 4.5f * s, main)
    c.ellipse(x - 8f * s, y + 1f, 6f * s, 3.4f * s, main)
    c.ellipse(x + 8f * s, y + 1.5f, 7f * s, 3.4f * s, main)
    c.ellipse(x + 1f, y - 3.5f * s, 7f * s, 4f * s, main)
    c.ellipse(x - 2f, y + 3.2f * s, 9f * s, 1.8f * s, shade)
}

private fun verticalGradient(
    c: PixelCanvas,
    y0: Int,
    y1: Int,
    top: Int,
    bottom: Int,
    bands: Int,
) {
    val span = (y1 - y0).coerceAtLeast(1)
    for (y in y0 until y1) {
        val t = (y - y0).toFloat() / span
        val band = (t * bands).toInt().coerceIn(0, bands - 1)
        val bt = if (bands <= 1) 0f else band.toFloat() / (bands - 1)
        val nextT = if (bands <= 1) 0f else (band + 1).toFloat() / (bands - 1)
        val base = mix(top, bottom, bt)
        val next = mix(top, bottom, nextT.coerceAtMost(1f))
        val frac = t * bands - band
        for (x in 0 until c.width) {
            val dither = frac > 0.55f && (x + y) % 2 == 0
            c.set(x, y, if (dither) next else base)
        }
    }
}

private fun mix(a: Int, b: Int, t: Float): Int {
    val ar = (a shr 16) and 0xFF
    val ag = (a shr 8) and 0xFF
    val ab = a and 0xFF
    val br = (b shr 16) and 0xFF
    val bg = (b shr 8) and 0xFF
    val bb = b and 0xFF
    val r = (ar + (br - ar) * t).toInt().coerceIn(0, 255)
    val g = (ag + (bg - ag) * t).toInt().coerceIn(0, 255)
    val bl = (ab + (bb - ab) * t).toInt().coerceIn(0, 255)
    return rgb(r, g, bl)
}
