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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.yzion.daiflix.tv.data.model.WatchHistory
import androidx.compose.ui.graphics.painter.ColorPainter
import com.yzion.daiflix.tv.ui.component.BottomScrim
import com.yzion.daiflix.tv.ui.component.DaiflixBackButton
import com.yzion.daiflix.tv.ui.component.EmptyState
import com.yzion.daiflix.tv.ui.component.RequestFocusWhenReady
import com.yzion.daiflix.tv.ui.component.focusBorder
import com.yzion.daiflix.tv.ui.component.rememberFocusState
import com.yzion.daiflix.tv.ui.component.trackFocus
import com.yzion.daiflix.tv.ui.theme.DaiflixColors
import com.yzion.daiflix.tv.ui.theme.DaiflixSpacing
import com.yzion.daiflix.tv.ui.viewmodel.HistoryViewModel

/**
 * Continue-watching shelf: poster grid with per-title progress, D-pad focusable.
 */
@Composable
fun HistoryScreen(
    onVodClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val history by viewModel.watchHistory.collectAsState()

    BackHandler(onBack = onBack)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DaiflixColors.Background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            LibraryHeader(
                title = "观看历史",
                subtitle = if (history.isEmpty()) null else "共 ${history.size} 部",
                onBack = onBack
            )

            if (history.isEmpty()) {
                val emptyActionFocus = remember { FocusRequester() }
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    EmptyState(
                        title = "还没有观看记录",
                        message = "去首页挑一部喜欢的片开始追剧吧",
                        actionLabel = "返回首页",
                        onAction = onBack,
                        actionFocusRequester = emptyActionFocus
                    )
                }
            } else {
                HistoryGrid(
                    history = history,
                    onVodClick = onVodClick,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun LibraryHeader(
    title: String,
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
        DaiflixBackButton(onBack = onBack)

        Spacer(modifier = Modifier.width(DaiflixSpacing.lg))

        Text(
            text = title,
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
private fun HistoryGrid(
    history: List<WatchHistory>,
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
        // vodId is the DB primary key, so it can never collide (the model id used
        // to be 0 for every row and crashed the grid with duplicate keys).
        itemsIndexed(history, key = { _, entry -> entry.vodId }) { index, entry ->
            HistoryCard(
                entry = entry,
                width = cardWidth,
                focusRequester = if (index == initialFocusIndex) firstCardFocusRequester else null,
                onClick = { onVodClick(entry.vodId) }
            )
        }
    }
}

@Composable
private fun HistoryCard(
    entry: WatchHistory,
    width: Dp,
    focusRequester: FocusRequester?,
    onClick: () -> Unit
) {
    val focus = rememberFocusState()
    val cardModifier = (if (focusRequester != null) {
        Modifier.width(width).focusRequester(focusRequester)
    } else {
        Modifier.width(width)
    }).trackFocus(focus)

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
        border = ClickableSurfaceDefaults.border(
            border = if (focus.isFocused) focusBorder() else Border.None,
            focusedBorder = focusBorder()
        )
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
                    model = entry.coverUrl,
                    contentDescription = entry.title,
                    contentScale = ContentScale.Crop,
                    placeholder = ColorPainter(DaiflixColors.DarkSurface),
                    error = ColorPainter(DaiflixColors.DarkCard),
                    modifier = Modifier.fillMaxSize()
                )
                BottomScrim(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .align(Alignment.BottomCenter)
                )
                if (entry.progress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .align(Alignment.BottomCenter)
                            .background(DaiflixColors.DarkGray.copy(alpha = 0.85f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(entry.progress.coerceIn(0f, 1f))
                                .height(4.dp)
                                .background(DaiflixColors.Red)
                        )
                    }
                }
            }

            Text(
                text = entry.title,
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

            val episodeLabel = "第${entry.episodeIndex + 1}集"
            val percent = (entry.progress * 100).toInt().coerceIn(0, 100)
            Text(
                text = if (percent > 0) "$episodeLabel · 已看 $percent%" else episodeLabel,
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
