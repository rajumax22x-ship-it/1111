package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val JarvisDarkColorScheme = darkColorScheme(
    primary = JarvisCyan,
    secondary = JarvisGold,
    background = JarvisDarkBackground,
    surface = JarvisSurface,
    surfaceVariant = JarvisSurfaceVariant,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = JarvisTextPrimary,
    onSurface = JarvisTextPrimary,
    error = JarvisError
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // JARVIS HUD defaults to immersive dark mode
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = JarvisDarkColorScheme,
        typography = Typography,
        content = content
    )
}
