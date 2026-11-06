package com.yzion.daiflix.tv.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Daiflix palette — a Netflix-adjacent dark theme.
 *
 * The UI is deliberately restrained: near-black backgrounds, white type, and a
 * single accent red used for brand, primary actions and live/episode markers.
 */
object DaiflixColors {
    // Brand
    val Red = Color(0xFFE50914)
    val RedPressed = Color(0xFFB00610)
    val DarkRed = Color(0xFFB20710)

    // Surfaces (near-black to dark grey, Netflix style)
    val Black = Color(0xFF000000)
    val Background = Color(0xFF0B0B0B)
    val DarkBackground = Color(0xFF141414)
    val DarkSurface = Color(0xFF1A1A1A)
    val DarkCard = Color(0xFF232323)
    val SurfaceElevated = Color(0xFF2B2B2B)
    val Divider = Color(0xFF2A2A2A)

    // Type
    val White = Color(0xFFFFFFFF)
    val LightGray = Color(0xFFB3B3B3)
    val MediumGray = Color(0xFF808080)
    val DarkGray = Color(0xFF4D4D4D)
    val TextPrimary = White
    val TextSecondary = LightGray
    val TextTertiary = Color(0xFF7A7A7A)

    // Accents
    val Gold = Color(0xFFFFC94D)
    val Green = Color(0xFF46D369)
    val Blue = Color(0xFF4A90E2)
    val Amber = Color(0xFFFFB300)

    // Scrims
    val Scrim = Color(0x99000000)
    val ScrimStrong = Color(0xE6000000)
    val ScrimSoft = Color(0x66000000)

    // Focus
    val FocusRing = White
    val FocusGlow = Color(0x66000000)

    // Gradients
    val GradientStart = Color(0xFF0B0B0B)
    val GradientEnd = Color(0x000B0B0B)
}
