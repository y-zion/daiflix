package com.y_zion.daiflix.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.y_zion.daiflix.data.model.Category
import com.y_zion.daiflix.data.model.VodItem
import com.y_zion.daiflix.ui.component.*
import com.y_zion.daiflix.ui.theme.DaiflixColors
import com.y_zion.daiflix.ui.theme.DaiflixSpacing
import com.y_zion.daiflix.ui.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    onVodClick: (String) -> Unit,
    onCategoryClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onHistoryClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DaiflixColors.Black)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopNavBar(
                selectedCategory = Category.HOME,
                onCategoryClick = { category ->
                    if (category == Category.HOME) viewModel.refresh()
                    else onCategoryClick(category.value)
                },
                onSearchClick = onSearchClick,
                onSettingsClick = onSettingsClick
            )

            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Text("加载中...", color = DaiflixColors.LightGray)
                    }
                }
                uiState.error != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                            Text(uiState.error ?: "加载失败", color = DaiflixColors.Red)
                            Spacer(modifier = Modifier.height(DaiflixSpacing.md))
                            Text("按 OK 重试", color = DaiflixColors.LightGray)
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = DaiflixSpacing.xxl)
                    ) {
                        if (uiState.banners.isNotEmpty()) {
                            item {
                                HeroBanner(
                                    item = uiState.banners.first(),
                                    onClick = { onVodClick(uiState.banners.first().id) },
                                    modifier = Modifier.padding(
                                        horizontal = DaiflixSpacing.lg,
                                        vertical = DaiflixSpacing.md
                                    )
                                )
                            }
                        }

                        if (uiState.watchHistory.isNotEmpty()) {
                            item {
                                VodRow(
                                    title = "继续观看",
                                    items = uiState.watchHistory.map {
                                        VodItem(
                                            id = it.vodId,
                                            title = it.title,
                                            coverUrl = it.coverUrl,
                                            category = Category.HOME
                                        )
                                    },
                                    onVodClick = onVodClick,
                                    modifier = Modifier.padding(top = DaiflixSpacing.md)
                                )
                            }
                        }

                        items(uiState.rows) { row ->
                            VodRow(
                                title = row.title,
                                items = row.items,
                                onVodClick = onVodClick,
                                modifier = Modifier.padding(top = DaiflixSpacing.md)
                            )
                        }
                    }
                }
            }
        }
    }
}
