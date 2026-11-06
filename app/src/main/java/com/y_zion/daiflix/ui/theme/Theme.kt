package com.y_zion.daiflix.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.tv.material3.MaterialTheme
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
    background = DaiflixColors.Black,
    onBackground = DaiflixColors.White,
    surface = DaiflixColors.DarkBackground,
    onSurface = DaiflixColors.White,
    surfaceVariant = DaiflixColors.DarkSurface,
    onSurfaceVariant = DaiflixColors.LightGray,
    error = DaiflixColors.Red,
    onError = DaiflixColors.White
)

@Composable
fun DaiflixTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = DaiflixTypography,
        content = content
    )
}
