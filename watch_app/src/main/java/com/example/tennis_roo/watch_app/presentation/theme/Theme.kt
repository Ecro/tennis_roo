package com.example.tennis_roo.watch_app.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

// Tennis-specific colors
private val TennisGreen = Color(0xFF2E7D32)
private val TennisYellow = Color(0xFFFBC02D)
private val TennisRed = Color(0xFFD32F2F)
private val TennisBlue = Color(0xFF1976D2)

// Dark theme colors
private val DarkColors = Colors(
    primary = TennisYellow,
    primaryVariant = Color(0xFFC49000),
    secondary = TennisBlue,
    secondaryVariant = Color(0xFF0D47A1),
    error = TennisRed,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onError = Color.White,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color(0xFF121212),
    onSurface = Color.White
)

// Light theme colors (for accessibility)
private val LightColors = Colors(
    primary = TennisGreen,
    primaryVariant = Color(0xFF1B5E20),
    secondary = TennisBlue,
    secondaryVariant = Color(0xFF0D47A1),
    error = TennisRed,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onError = Color.White,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color(0xFFF5F5F5),
    onSurface = Color.Black
)

// Color-blind friendly palette
private val ColorBlindColors = Colors(
    primary = Color(0xFF4D85BD), // Blue that works for most color blindness types
    primaryVariant = Color(0xFF2D5E8D),
    secondary = Color(0xFFE69F00), // Orange that works for most color blindness types
    secondaryVariant = Color(0xFFB67D00),
    error = Color(0xFFCC79A7), // Pink/purple that works for most color blindness types
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onError = Color.White,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color(0xFF121212),
    onSurface = Color.White
)

@Composable
fun TennisRooTheme(
    darkTheme: Boolean = true,
    colorBlindMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = when {
        colorBlindMode -> ColorBlindColors
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colors = colors,
        content = content
    )
}
