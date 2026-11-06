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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.tv.material3.*
import com.y_zion.daiflix.data.model.VodItem
import com.y_zion.daiflix.data.repository.VodRepository
import com.y_zion.daiflix.ui.component.VodCard
import com.y_zion.daiflix.ui.theme.DaiflixColors
import com.y_zion.daiflix.ui.theme.DaiflixSpacing
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val repository: VodRepository
) : ViewModel() {
    val favorites by lazy {
        repository.getFavorites().stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            emptyList<VodItem>()
        )
    }
}

@Composable
fun FavoritesScreen(
    onVodClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val favorites by viewModel.favorites.collectAsState()

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
                    text = "\u6211\u7684\u6536\u85cf",
                    style = MaterialTheme.typography.headlineMedium,
                    color = DaiflixColors.White
                )
            }

            Spacer(modifier = Modifier.height(DaiflixSpacing.lg))

            if (favorites.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("\u6682\u65e0\u6536\u85cf\u5185\u5bb9", color = DaiflixColors.LightGray)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    horizontalArrangement = Arrangement.spacedBy(DaiflixSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(DaiflixSpacing.md)
                ) {
                    items(favorites, key = { it.id }) { vod ->
                        VodCard(item = vod, onClick = { onVodClick(vod.id) })
                    }
                }
            }
        }
    }
}
