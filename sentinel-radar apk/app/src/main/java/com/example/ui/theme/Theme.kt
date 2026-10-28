package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SentinelColorScheme = darkColorScheme(
    primary = RadarCyan,
    onPrimary = Color(0xFF020617),
    primaryContainer = RadarCyanDark,
    onPrimaryContainer = Color.White,
    secondary = RadarCyan,
    onSecondary = Color(0xFF020617),
    error = RadarRed,
    onError = Color.White,
    errorContainer = RadarRedDeep,
    onErrorContainer = Color(0xFFFECACA),
    background = RadarDarkBackground,
    onBackground = RadarTextPrimary,
    surface = RadarSurfaceVariant,
    onSurface = RadarTextPrimary,
    surfaceVariant = RadarCardBackground,
    onSurfaceVariant = RadarTextSecondary,
    outline = RadarBorder,
    outlineVariant = RadarBorderLight
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SentinelColorScheme,
        typography = Typography,
        content = content
    )
}
