package com.chatapp.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Light Theme
val LightBackground = Color(0xFFF5F5F5)
val LightSurface = Color(0xFFFAFAFA)
val LightSurfaceVariant = Color(0xFFEEEEEE)
val LightOutline = Color(0xFFE0E0E0)
val LightOnBackground = Color(0xFF1A1A1A)
val LightOnSurface = Color(0xFF616161)
val LightOnSurfaceVariant = Color(0xFF9E9E9E)
val LightPrimary = Color(0xFF1A1A1A)
val LightOnPrimary = Color(0xFFF5F5F5)

// Dark Theme
val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val DarkSurfaceVariant = Color(0xFF2C2C2C)
val DarkOutline = Color(0xFF383838)
val DarkOnBackground = Color(0xFFE8E8E8)
val DarkOnSurface = Color(0xFFA0A0A0)
val DarkOnSurfaceVariant = Color(0xFF6E6E6E)
val DarkPrimary = Color(0xFFE8E8E8)
val DarkOnPrimary = Color(0xFF121212)

// Accent (same in both themes)
val AccentBlue = Color(0xFF3B82F6)
val AccentGreen = Color(0xFF10B981)

val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline
)

val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline
)
