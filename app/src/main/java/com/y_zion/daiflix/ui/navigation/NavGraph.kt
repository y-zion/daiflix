package com.y_zion.daiflix.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import com.y_zion.daiflix.ui.screen.*

@Composable
fun DaiflixNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home,
        modifier = modifier
    ) {
        composable<Screen.Home> {
            HomeScreen(
                onVodClick = { vodId ->
                    navController.navigate(Screen.Detail(vodId))
                },
                onCategoryClick = { categoryId ->
                    navController.navigate(Screen.Category(categoryId))
                },
                onSearchClick = {
                    navController.navigate(Screen.Search)
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings)
                },
                onHistoryClick = {
                    navController.navigate(Screen.History)
                }
            )
        }

        composable<Screen.Category> { backStackEntry ->
            val category = backStackEntry.toRoute<Screen.Category>()
            CategoryScreen(
                categoryId = category.categoryId,
                onVodClick = { vodId ->
                    navController.navigate(Screen.Detail(vodId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable<Screen.Detail> { backStackEntry ->
            val detail = backStackEntry.toRoute<Screen.Detail>()
            DetailScreen(
                vodId = detail.vodId,
                onPlayClick = { vodId, sourceIdx, epIdx ->
                    navController.navigate(Screen.Player(vodId, sourceIdx, epIdx))
                },
                onVodClick = { vodId ->
                    navController.navigate(Screen.Detail(vodId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable<Screen.Player> { backStackEntry ->
            val player = backStackEntry.toRoute<Screen.Player>()
            PlayerScreen(
                vodId = player.vodId,
                sourceIndex = player.sourceIndex,
                episodeIndex = player.episodeIndex,
                onBack = { navController.popBackStack() },
                onEpisodeChange = { vodId, sourceIdx, epIdx ->
                    navController.navigate(Screen.Player(vodId, sourceIdx, epIdx)) {
                        popUpTo<Screen.Player> { inclusive = true }
                    }
                }
            )
        }

        composable<Screen.Search> {
            SearchScreen(
                onVodClick = { vodId ->
                    navController.navigate(Screen.Detail(vodId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable<Screen.Settings> {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable<Screen.History> {
            HistoryScreen(
                onVodClick = { vodId ->
                    navController.navigate(Screen.Detail(vodId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable<Screen.Favorites> {
            FavoritesScreen(
                onVodClick = { vodId ->
                    navController.navigate(Screen.Detail(vodId))
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
