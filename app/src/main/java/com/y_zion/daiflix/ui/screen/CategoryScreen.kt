package com.y_zion.daiflix.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.y_zion.daiflix.data.model.Category
import com.y_zion.daiflix.ui.component.TopNavBar
import com.y_zion.daiflix.ui.component.VodCard
import com.y_zion.daiflix.ui.theme.DaiflixColors
import com.y_zion.daiflix.ui.theme.DaiflixSpacing
import com.y_zion.daiflix.ui.viewmodel.CategoryViewModel

@Composable
fun CategoryScreen(
    categoryId: String,
    onVodClick: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onBack: () -> Unit,
    viewModel: CategoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val category = remember(categoryId) { Category.fromValue(categoryId) }

    LaunchedEffect(category) {
        viewModel.loadCategory(category)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DaiflixColors.Black)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopNavBar(
                selectedCategory = uiState.category,
                onCategoryClick = { cat ->
                    if (cat == Category.HOME) onBack()
                    else if (cat != uiState.category) onCategoryChange(cat.value)
                },
                onSearchClick = onSearchClick,
                onSettingsClick = onSettingsClick,
                onHistoryClick = onHistoryClick,
                onFavoritesClick = onFavoritesClick
            )

            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Text("加载中...", color = DaiflixColors.LightGray)
                    }
                }
                uiState.error != null && uiState.items.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                            Text(uiState.error ?: "加载失败", color = DaiflixColors.Red)
                            Spacer(modifier = Modifier.height(DaiflixSpacing.md))
                            Row(horizontalArrangement = Arrangement.spacedBy(DaiflixSpacing.sm)) {
                                Surface(
                                    onClick = { viewModel.loadCategory(category) },
                                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                                    colors = ClickableSurfaceDefaults.colors(
                                        containerColor = DaiflixColors.Red,
                                        focusedContainerColor = DaiflixColors.DarkRed
                                    )
                                ) {
                                    Text(
                                        text = "重试",
                                        color = DaiflixColors.White,
                                        modifier = Modifier.padding(
                                            horizontal = DaiflixSpacing.lg,
                                            vertical = DaiflixSpacing.xs
                                        )
                                    )
                                }
                                Surface(
                                    onClick = onBack,
                                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                                    colors = ClickableSurfaceDefaults.colors(
                                        containerColor = DaiflixColors.DarkCard
                                    )
                                ) {
                                    Text(
                                        text = "返回首页",
                                        color = DaiflixColors.White,
                                        modifier = Modifier.padding(
                                            horizontal = DaiflixSpacing.lg,
                                            vertical = DaiflixSpacing.xs
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        contentPadding = PaddingValues(horizontal = DaiflixSpacing.lg, vertical = DaiflixSpacing.md),
                        horizontalArrangement = Arrangement.spacedBy(DaiflixSpacing.sm),
                        verticalArrangement = Arrangement.spacedBy(DaiflixSpacing.md),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.items, key = { it.id }) { item ->
                            VodCard(item = item, onClick = { onVodClick(item.id) })
                        }
                    }
                }
            }
        }
    }
}
