package com.yzion.daiflix.tv.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.yzion.daiflix.tv.data.model.VodItem
import com.yzion.daiflix.tv.data.repository.VodRepository
import com.yzion.daiflix.tv.ui.component.DaiflixBackButton
import com.yzion.daiflix.tv.ui.component.EmptyState
import com.yzion.daiflix.tv.ui.component.RequestFocusWhenReady
import com.yzion.daiflix.tv.ui.component.focusBorder
import com.yzion.daiflix.tv.ui.theme.DaiflixColors
import com.yzion.daiflix.tv.ui.theme.DaiflixSpacing
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

/**
 * "我的收藏" shelf: 6-column poster grid with focus ring, empty state offers a way back.
 */
@Composable
fun FavoritesScreen(
    onVodClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val favorites by viewModel.favorites.collectAsState()
    // 空片单只剩一个「去逛逛」按钮，首帧焦点必须落到它上面，否则遥控器无处可去。
    val emptyActionFocus = remember { FocusRequester() }

    BackHandler(onBack = onBack)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DaiflixColors.Background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            FavoritesHeader(
                subtitle = if (favorites.isEmpty()) null else "共 ${favorites.size} 部",
                onBack = onBack
            )

            if (favorites.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    EmptyState(
                        title = "还没有收藏内容",
                        message = "在详情页点按「收藏」，片单会出现在这里",
                        actionLabel = "去逛逛",
                        onAction = onBack,
                        actionFocusRequester = emptyActionFocus
                    )
                }
            } else {
                FavoritesGrid(
                    favorites = favorites,
                    onVodClick = onVodClick,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun FavoritesHeader(
    subtitle: String?,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = DaiflixSpacing.screenPadding,
                end = DaiflixSpacing.screenPadding,
                top = DaiflixSpacing.lg,
                bottom = DaiflixSpacing.lg
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 返回圆钮统一走共享组件：黑 55% 圆底 + 白箭头 + hairline，焦点白底黑箭头 + focusHalo，
        // 与历史页/详情页等保持一致（审计 P3-4）。
        DaiflixBackButton(onBack = onBack)

        Spacer(modifier = Modifier.width(DaiflixSpacing.lg))

        Text(
            text = "我的收藏",
            style = MaterialTheme.typography.headlineLarge,
            color = DaiflixColors.White
        )

        if (subtitle != null) {
            Spacer(modifier = Modifier.width(DaiflixSpacing.md))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = DaiflixColors.MediumGray
            )
        }
    }
}

@Composable
private fun FavoritesGrid(
    favorites: List<VodItem>,
    onVodClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val gridState = rememberLazyGridState()
    // Restore focus to the card the user was looking at when coming back from detail.
    val initialFocusIndex = remember { gridState.firstVisibleItemIndex }
    val firstCardFocusRequester = remember { FocusRequester() }
    val spacing = DaiflixSpacing.md
    val config = LocalConfiguration.current
    val cardWidth = ((config.screenWidthDp - 2 * DaiflixSpacing.screenPadding.value - 5 * spacing.value) / 6)
        .coerceAtLeast(120f)
        .dp

    // 首帧之后再请求焦点：等卡片的聚焦订阅建立，焦点样式第一帧即可见（审计 P1-3）。
    RequestFocusWhenReady(firstCardFocusRequester)

    LazyVerticalGrid(
        columns = GridCells.Fixed(6),
        state = gridState,
        modifier = modifier,
        contentPadding = PaddingValues(
            start = DaiflixSpacing.screenPadding,
            end = DaiflixSpacing.screenPadding,
            bottom = DaiflixSpacing.xxxl
        ),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalArrangement = Arrangement.spacedBy(DaiflixSpacing.lg)
    ) {
        itemsIndexed(favorites, key = { _, item -> item.id }) { index, item ->
            FavoriteCard(
                item = item,
                width = cardWidth,
                focusRequester = if (index == initialFocusIndex) firstCardFocusRequester else null,
                onClick = { onVodClick(item.id) }
            )
        }
    }
}

@Composable
private fun FavoriteCard(
    item: VodItem,
    width: Dp,
    focusRequester: FocusRequester?,
    onClick: () -> Unit
) {
    val cardModifier = if (focusRequester != null) {
        Modifier.width(width).focusRequester(focusRequester)
    } else {
        Modifier.width(width)
    }

    Surface(
        onClick = onClick,
        modifier = cardModifier,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = DaiflixColors.DarkCard,
            contentColor = DaiflixColors.White,
            focusedContainerColor = DaiflixColors.DarkCard,
            focusedContentColor = DaiflixColors.White
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
        border = ClickableSurfaceDefaults.border(focusedBorder = focusBorder())
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                    .background(DaiflixColors.DarkSurface)
            ) {
                AsyncImage(
                    model = item.coverUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                color = DaiflixColors.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(
                    start = DaiflixSpacing.sm,
                    end = DaiflixSpacing.sm,
                    top = DaiflixSpacing.xs
                )
            )

            val meta = listOfNotNull(
                item.year.takeIf { it.isNotBlank() },
                item.rating.takeIf { it.isNotBlank() }?.let { "★ $it" },
                item.category.displayName.takeIf { it.isNotBlank() }
            ).joinToString(" · ")

            Text(
                text = meta,
                style = MaterialTheme.typography.labelSmall,
                color = DaiflixColors.LightGray,
                maxLines = 1,
                modifier = Modifier.padding(
                    start = DaiflixSpacing.sm,
                    end = DaiflixSpacing.sm,
                    top = 2.dp,
                    bottom = DaiflixSpacing.sm
                )
            )
        }
    }
}
