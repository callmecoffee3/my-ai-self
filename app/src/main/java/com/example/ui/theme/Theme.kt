package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VibrantColorScheme = lightColorScheme(
    primary = VibrantPrimary,
    onPrimary = VibrantOnPrimary,
    primaryContainer = VibrantPrimaryContainer,
    onPrimaryContainer = VibrantOnPrimaryContainer,
    secondaryContainer = VibrantSecondaryContainer,
    onSecondaryContainer = VibrantOnSecondaryContainer,
    background = VibrantBackground,
    onBackground = VibrantTextPrimary,
    surface = VibrantSurface,
    onSurface = VibrantTextPrimary,
    surfaceVariant = VibrantSurfaceVariant,
    onSurfaceVariant = VibrantTextSecondary,
    outline = VibrantBorder,
    outlineVariant = VibrantBorderLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Use Vibrant light aesthetic
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = VibrantColorScheme,
        typography = Typography,
        content = content
    )
}
