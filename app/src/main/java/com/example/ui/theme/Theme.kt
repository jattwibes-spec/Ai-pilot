package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val WibesDarkColorScheme = darkColorScheme(
    primary = ElectricViolet,
    onPrimary = Color.White,
    primaryContainer = ElectricVioletDark,
    onPrimaryContainer = ElectricVioletLight,
    secondary = ElectricVioletLight,
    onSecondary = DarkGraphite,
    secondaryContainer = SurfaceElevated,
    onSecondaryContainer = TextPrimary,
    tertiary = AccentInfo,
    onTertiary = DarkGraphite,
    background = DarkGraphite,
    onBackground = TextPrimary,
    surface = DeepBlackSurface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = SurfaceBorder,
    outlineVariant = SurfaceBorder,
    error = AccentError,
    onError = Color.White
)

@Composable
fun WibesAutopilotTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = WibesDarkColorScheme,
        typography = Typography,
        content = content
    )
}
