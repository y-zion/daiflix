package com.y_zion.daiflix.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.*
import com.y_zion.daiflix.data.model.Category
import com.y_zion.daiflix.data.model.VodItem
import com.y_zion.daiflix.ui.component.VodCard
import com.y_zion.daiflix.ui.theme.DaiflixColors
import com.y_zion.daiflix.ui.theme.DaiflixSpacing
import com.y_zion.daiflix.ui.viewmodel.HomeViewModel

@Composable
fun HistoryScreen(
    onVodClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DaiflixColors.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(DaiflixSpacing.lg)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onBack,
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(50)),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("\u2190", color = DaiflixColors.White, style = MaterialTheme.typography.titleLarge)
                    }
                }
                Spacer(modifier = Modifier.width(DaiflixSpacing.md))
                Text(
                    text = "\u89c2\u770b\u5386\u53f2",
                    style = MaterialTheme.typography.headlineMedium,
                    color = DaiflixColors.White
                )
            }

            Spacer(modifier = Modifier.height(DaiflixSpacing.lg))

            if (uiState.watchHistory.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("\u6682\u65e0\u89c2\u770b\u8bb0\u5f55", color = DaiflixColors.LightGray)
                }
            } else {
                val historyItems = uiState.watchHistory.map { history ->
                    VodItem(
                        id = history.vodId,
                        title = history.title,
                        coverUrl = history.coverUrl,
                        category = Category.HOME
                    )
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    horizontalArrangement = Arrangement.spacedBy(DaiflixSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(DaiflixSpacing.md)
                ) {
                    items(historyItems, key = { it.id }) { vod ->
                        VodCard(item = vod, onClick = { onVodClick(vod.id) })
                    }
                }
            }
        }
    }
}
