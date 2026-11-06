package com.yzion.daiflix.tv.ui.theme

import androidx.compose.ui.unit.dp

// TV-optimized spacing (larger for living room viewing)
object DaiflixSpacing {
    val xxxs = 2.dp
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
    val xxxl = 64.dp

    /** Standard left/right gutter for full-bleed rows and page content. */
    val screenPadding = 48.dp

    /** Vertical gap between content rows. */
    val rowGap = 28.dp
}

// Card dimensions — Netflix-style portrait key art (2:3).
object CardDimensions {
    val posterWidth = 180.dp
    val posterHeight = 270.dp
    val backdropWidth = 320.dp
    val backdropHeight = 180.dp
    val landscapeWidth = 320.dp
    val landscapeHeight = 180.dp
    val cardCornerRadius = 10.dp
    val cardElevation = 0.dp

    /** How much a card grows when it receives TV focus. */
    const val focusedScale = 1.08f
}

// Hero banner dimensions
object BannerDimensions {
    val heroHeight = 520.dp
    val heroGradientHeight = 260.dp
}

// Navigation
object NavigationDimensions {
    val topNavHeight = 76.dp
    val sideNavWidth = 72.dp
}
