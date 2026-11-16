package com.cutetool.mestyachkovoe.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.cutetool.mestyachkovoe.data.ThemeMode

private val MonoDark = darkColorScheme(
    primary = Color(0xFFEDEDED),
    onPrimary = Color(0xFF111111),
    primaryContainer = Color(0xFF333333),
    onPrimaryContainer = Color(0xFFEDEDED),
    secondary = Color(0xFFBDBDBD),
    background = Color(0xFF101010),
    surface = Color(0xFF101010),
    surfaceVariant = Color(0xFF1E1E1E),
    onSurface = Color(0xFFEDEDED),
    onSurfaceVariant = Color(0xFFBDBDBD),
    outline = Color(0xFF777777)
)

private val ClassicLight = lightColorScheme(
    primary = Color(0xFF1565C0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E4FF),
    onPrimaryContainer = Color(0xFF001C3A),
    secondary = Color(0xFF455A64),
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF2F2F2),
    onSurface = Color(0xFF111111),
    onSurfaceVariant = Color(0xFF444444),
    outline = Color(0xFF888888)
)

@Composable
fun MestyachkovoeTheme(
    mode: ThemeMode,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val dark = isSystemInDarkTheme()
    val colors = when (mode) {
        ThemeMode.MONO -> MonoDark
        ThemeMode.LIGHT -> ClassicLight
        ThemeMode.MONET -> when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dark -> dynamicDarkColorScheme(context)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
            dark -> darkColorScheme()
            else -> lightColorScheme()
        }
    }
    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}
