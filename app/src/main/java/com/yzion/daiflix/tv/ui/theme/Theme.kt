package com.yzion.daiflix.tv.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Shapes
import androidx.tv.material3.darkColorScheme

// Netflix-inspired dark color scheme
private val DarkColorScheme = darkColorScheme(
    primary = DaiflixColors.Red,
    onPrimary = DaiflixColors.White,
    primaryContainer = DaiflixColors.DarkRed,
    onPrimaryContainer = DaiflixColors.White,
    secondary = DaiflixColors.LightGray,
    onSecondary = DaiflixColors.Black,
    secondaryContainer = DaiflixColors.DarkCard,
    onSecondaryContainer = DaiflixColors.White,
    tertiary = DaiflixColors.Green,
    onTertiary = DaiflixColors.White,
    tertiaryContainer = DaiflixColors.DarkSurface,
    onTertiaryContainer = DaiflixColors.White,
    background = DaiflixColors.Background,
    onBackground = DaiflixColors.White,
    surface = DaiflixColors.DarkBackground,
    onSurface = DaiflixColors.White,
    surfaceVariant = DaiflixColors.DarkSurface,
    onSurfaceVariant = DaiflixColors.LightGray,
    error = DaiflixColors.Red,
    onError = DaiflixColors.White
)

private val DaiflixShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun DaiflixTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = DaiflixTypography,
        shapes = DaiflixShapes,
        content = content
    )
}
