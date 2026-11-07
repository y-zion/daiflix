package com.yzion.daiflix.tv.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.yzion.daiflix.tv.data.model.VodDetail
import com.yzion.daiflix.tv.ui.component.BottomScrim
import com.yzion.daiflix.tv.ui.component.DaiflixPrimaryButton
import com.yzion.daiflix.tv.ui.component.DaiflixSecondaryButton
import com.yzion.daiflix.tv.ui.component.EpisodeSelector
import com.yzion.daiflix.tv.ui.component.HorizontalScrim
import com.yzion.daiflix.tv.ui.component.LoadingState
import com.yzion.daiflix.tv.ui.component.MetaPill
import com.yzion.daiflix.tv.ui.component.RequestFocusWhenReady
import com.yzion.daiflix.tv.ui.component.SectionHeader
import com.yzion.daiflix.tv.ui.component.TopScrim
import com.yzion.daiflix.tv.ui.component.VodRow
import com.yzion.daiflix.tv.ui.component.focusBorder
import com.yzion.daiflix.tv.ui.component.focusHalo
import com.yzion.daiflix.tv.ui.component.rememberFocusState
import com.yzion.daiflix.tv.ui.component.trackFocus
import com.yzion.daiflix.tv.ui.theme.BannerDimensions
import com.yzion.daiflix.tv.ui.theme.CardDimensions
import com.yzion.daiflix.tv.ui.theme.DaiflixColors
import com.yzion.daiflix.tv.ui.theme.DaiflixSpacing
import com.yzion.daiflix.tv.ui.viewmodel.DetailUiState
import com.yzion.daiflix.tv.ui.viewmodel.DetailViewModel

/**
 * Netflix-style TV detail page: full-bleed backdrop hero, poster + metadata +
 * actions over the artwork, then an episode picker and related carousel.
 */
@Composable
fun DetailScreen(
    vodId: String,
    onPlayClick: (String, Int, Int) -> Unit,
    onVodClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    BackHandler(onBack = onBack)

    // Refresh resume state when returning from the player.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshWatchHistory()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DaiflixColors.Background)
    ) {
        val detail = uiState.detail
        when {
            uiState.isLoading && detail == null -> LoadingState()
            detail == null -> DetailErrorState(
                message = uiState.error,
                onRetry = { viewModel.refresh() },
                onBack = onBack
            )
            else -> DetailContent(
                detail = detail,
                uiState = uiState,
                onPlayClick = onPlayClick,
                onVodClick = onVodClick,
                onBack = onBack,
                onToggleFavorite = { viewModel.toggleFavorite() },
                onSourceChange = { index -> viewModel.selectSource(index) },
                onEpisodeClick = { episodeIndex ->
                    viewModel.selectEpisode(episodeIndex)
                    onPlayClick(vodId, uiState.selectedSourceIndex, episodeIndex)
                }
            )
        }
    }
}

@Composable
private fun DetailContent(
    detail: VodDetail,
    uiState: DetailUiState,
    onPlayClick: (String, Int, Int) -> Unit,
    onVodClick: (String) -> Unit,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSourceChange: (Int) -> Unit,
    onEpisodeClick: (Int) -> Unit
) {
    val selectedSource = detail.playSources.getOrNull(uiState.selectedSourceIndex)
    val episodes = selectedSource?.episodes.orEmpty()
    val selectedEpisode = episodes.getOrNull(uiState.selectedEpisodeIndex)
    val history = uiState.watchHistory
    val canResume = history != null &&
        history.position > 0 &&
        history.sourceIndex == uiState.selectedSourceIndex &&
        history.episodeIndex == uiState.selectedEpisodeIndex
    val episodeLabel = selectedEpisode?.name?.takeIf { it.isNotBlank() }
        ?: "第${uiState.selectedEpisodeIndex + 1}集"
    val playLabel = if (canResume) "继续播放 $episodeLabel" else "播放"

    val playFocusRequester = remember { FocusRequester() }
    val backFocusRequester = remember { FocusRequester() }
    val backFocus = rememberFocusState()
    val scrollState = rememberScrollState()

    // Enter the page pinned to the top. Focus now lands on the hero back button
    // (below) instead of the in-body play button; the old LaunchedEffect that
    // grabbed the play button made verticalScroll auto-scroll the hero partly
    // off-screen right on entry (audit P1-5).
    LaunchedEffect(detail.id) {
        runCatching { scrollState.scrollTo(0) }
    }

    // First-frame focus on the back button, which is visible at scroll 0, so
    // requesting it never scrolls the container.
    RequestFocusWhenReady(backFocusRequester, key = detail.id)

    val backdrop = detail.backdropUrl.ifBlank { detail.coverUrl }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // ---- Hero ----
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(BannerDimensions.heroHeight)
        ) {
            AsyncImage(
                model = backdrop,
                contentDescription = detail.title,
                contentScale = ContentScale.Crop,
                placeholder = ColorPainter(DaiflixColors.DarkSurface),
                error = ColorPainter(DaiflixColors.DarkCard),
                modifier = Modifier.fillMaxSize()
            )
            HorizontalScrim(modifier = Modifier.fillMaxSize())
            BottomScrim(modifier = Modifier.fillMaxSize(), strength = 1f)
            TopScrim(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .align(Alignment.TopCenter)
            )

            // Floating back button. Focus styling is driven by the explicit
            // rememberFocusState() (onFocusChanged) instead of the tv-material
            // interactionSource focused colors: on the very first frame the
            // first FocusInteraction can be missed, leaving a focused button
            // that does not look focused (audit P1-3).
            Surface(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = DaiflixSpacing.screenPadding, top = DaiflixSpacing.md)
                    .size(48.dp)
                    .focusRequester(backFocusRequester)
                    .trackFocus(backFocus)
                    .focusHalo(backFocus, cornerRadius = 24.dp)
                    .onPreviewKeyEvent { event ->
                        // Poster column sits between the back button and the
                        // action row, so DOWN is routed explicitly instead of
                        // relying on geometric focus search.
                        if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown) {
                            // Only swallow the event when the move actually
                            // happened; otherwise fall back to default focus
                            // search so focus never gets stuck on the back
                            // button.
                            runCatching { playFocusRequester.requestFocus() }.isSuccess
                        } else {
                            false
                        }
                    },
                shape = ClickableSurfaceDefaults.shape(CircleShape),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = if (backFocus.isFocused) {
                        DaiflixColors.White
                    } else {
                        DaiflixColors.Black.copy(alpha = 0.55f)
                    },
                    contentColor = if (backFocus.isFocused) {
                        DaiflixColors.Black
                    } else {
                        DaiflixColors.White
                    },
                    focusedContainerColor = DaiflixColors.White,
                    focusedContentColor = DaiflixColors.Black
                ),
                border = ClickableSurfaceDefaults.border(
                    border = if (backFocus.isFocused) {
                        focusBorder(DaiflixColors.Black)
                    } else {
                        Border.None
                    }
                ),
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                        contentDescription = "返回",
                        modifier = Modifier.size(24.dp),
                        tint = if (backFocus.isFocused) DaiflixColors.Black else DaiflixColors.White
                    )
                }
            }

            // Poster + copy at the bottom of the artwork.
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(
                        start = DaiflixSpacing.screenPadding,
                        end = DaiflixSpacing.screenPadding,
                        bottom = DaiflixSpacing.xxl
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(CardDimensions.posterWidth)
                        .height(CardDimensions.posterHeight)
                        .clip(RoundedCornerShape(CardDimensions.cardCornerRadius))
                        .background(DaiflixColors.DarkCard)
                ) {
                    AsyncImage(
                        model = detail.coverUrl,
                        contentDescription = detail.title,
                        contentScale = ContentScale.Crop,
                        placeholder = ColorPainter(DaiflixColors.DarkSurface),
                        error = ColorPainter(DaiflixColors.DarkCard),
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(DaiflixSpacing.xl))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = detail.title,
                        style = MaterialTheme.typography.displaySmall,
                        color = DaiflixColors.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(DaiflixSpacing.sm))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(DaiflixSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MetaPill(detail.category.displayName)
                        if (detail.year.isNotBlank()) MetaPill(detail.year)
                        if (detail.region.isNotBlank()) MetaPill(detail.region)
                        if (detail.rating.isNotBlank()) {
                            MetaPill("★ ${detail.rating}", color = DaiflixColors.Gold)
                        }
                        if (episodes.isNotEmpty()) MetaPill("全 ${episodes.size} 集")
                        if (canResume) {
                            val percent = if (history != null && history.duration > 0) {
                                (history.position * 100 / history.duration).toInt().coerceIn(1, 99)
                            } else {
                                -1
                            }
                            if (percent > 0) {
                                MetaPill("已观看 $percent%", color = DaiflixColors.Green)
                            }
                        }
                    }

                    if (detail.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(DaiflixSpacing.md))
                        Text(
                            text = detail.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = DaiflixColors.LightGray,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(DaiflixSpacing.lg))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(DaiflixSpacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DaiflixPrimaryButton(
                            text = playLabel,
                            onClick = {
                                onPlayClick(
                                    detail.id,
                                    uiState.selectedSourceIndex,
                                    uiState.selectedEpisodeIndex
                                )
                            },
                            icon = Icons.Rounded.PlayArrow,
                            // Always reachable from the back button via the
                            // explicit DOWN handler above.
                            modifier = Modifier.focusRequester(playFocusRequester)
                        )

                        DaiflixSecondaryButton(
                            text = if (uiState.isFavorite) "已收藏" else "收藏",
                            onClick = onToggleFavorite,
                            icon = if (uiState.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder
                        )
                    }
                }
            }
        }

        // ---- Episodes ----
        if (detail.playSources.isNotEmpty()) {
            Spacer(modifier = Modifier.height(DaiflixSpacing.lg))
            SectionHeader(
                title = "选集播放",
                trailing = {
                    Text(
                        text = if (episodes.isEmpty()) "暂无剧集" else "共 ${episodes.size} 集",
                        style = MaterialTheme.typography.labelMedium,
                        color = DaiflixColors.LightGray
                    )
                }
            )
            Spacer(modifier = Modifier.height(DaiflixSpacing.sm))
            EpisodeSelector(
                sources = detail.playSources,
                selectedSourceIndex = uiState.selectedSourceIndex,
                selectedEpisodeIndex = uiState.selectedEpisodeIndex,
                onSourceChange = onSourceChange,
                onEpisodeClick = onEpisodeClick,
                modifier = Modifier.padding(horizontal = DaiflixSpacing.screenPadding),
                lastWatchedSourceIndex = history?.sourceIndex ?: -1,
                lastWatchedEpisodeIndex = history?.episodeIndex ?: -1
            )
        }

        // ---- Related ----
        if (detail.relatedItems.isNotEmpty()) {
            Spacer(modifier = Modifier.height(DaiflixSpacing.xxl))
            VodRow(
                title = "相关推荐",
                items = detail.relatedItems,
                onVodClick = onVodClick
            )
        }

        Spacer(modifier = Modifier.height(DaiflixSpacing.xxxl))
    }
}

@Composable
private fun DetailErrorState(
    message: String?,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    val retryFocusRequester = remember { FocusRequester() }
    RequestFocusWhenReady(retryFocusRequester)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DaiflixColors.Background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "加载失败",
                style = MaterialTheme.typography.headlineMedium,
                color = DaiflixColors.White
            )
            Spacer(modifier = Modifier.height(DaiflixSpacing.sm))
            Text(
                text = message ?: "请检查网络后重试",
                style = MaterialTheme.typography.bodyLarge,
                color = DaiflixColors.LightGray
            )
            Spacer(modifier = Modifier.height(DaiflixSpacing.lg))
            Row(horizontalArrangement = Arrangement.spacedBy(DaiflixSpacing.md)) {
                DaiflixPrimaryButton(
                    text = "重试",
                    onClick = onRetry,
                    modifier = Modifier.focusRequester(retryFocusRequester)
                )
                DaiflixSecondaryButton(text = "返回", onClick = onBack)
            }
        }
    }
}
