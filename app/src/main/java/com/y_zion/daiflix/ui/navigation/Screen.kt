package com.y_zion.daiflix.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class Screen {
    @Serializable
    data object Home : Screen()

    @Serializable
    data class Category(val categoryId: String = "movie") : Screen()

    @Serializable
    data class Detail(val vodId: String) : Screen()

    @Serializable
    data class Player(
        val vodId: String,
        val sourceIndex: Int = 0,
        val episodeIndex: Int = 0
    ) : Screen()

    @Serializable
    data object Search : Screen()

    @Serializable
    data object Settings : Screen()

    @Serializable
    data object History : Screen()

    @Serializable
    data object Favorites : Screen()
}
