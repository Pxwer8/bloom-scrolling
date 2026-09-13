package com.hackwestx.bloomscrolling.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Fixed light palette on purpose — this app defines its own periwinkle
// identity in Color.kt, so it deliberately does not follow system dark theme
// or Android 12+ dynamic color (both would override these tokens).
private val BloomColorScheme = lightColorScheme(
    primary = ColorAccent,
    onPrimary = ColorTextOnAccent,
    secondary = ColorAccentSoft,
    onSecondary = ColorTextPrimary,
    background = ColorBg,
    onBackground = ColorTextPrimary,
    surface = ColorSurface,
    onSurface = ColorTextPrimary,
    surfaceVariant = ColorSurfaceAlt,
    onSurfaceVariant = ColorTextSecondary,
    outline = ColorBorder,
    error = ColorNegative,
    onError = ColorTextOnAccent
)

@Composable
fun BloomScrollingTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = BloomColorScheme,
        typography = Typography,
        content = content
    )
}