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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.yzion.daiflix.tv.data.model.VodDetail
import com.yzion.daiflix.tv.ui.component.DaiflixBackButton
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
    // P2-4: the 展开/收起 toggle sits between the synopsis and the action row,
    // so the default 2D focus search skips it; wire it explicitly.
    val expandFocusRequester = remember { FocusRequester() }
    val backFocusRequester = remember { FocusRequester() }
    val scrollState = rememberScrollState()

    // P2-4: the synopsis starts clamped to 3 lines; the toggle only appears
    // when the clamped text actually overflows (measured via onTextLayout).
    var descriptionExpanded by remember(detail.id, detail.description) { mutableStateOf(false) }
    var descriptionOverflows by remember(detail.id, detail.description) { mutableStateOf(false) }

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

    // P2-1: the site sometimes stuffs a full release-date list into `year`
    // (e.g. "2023-08-08(中国大陆),2023-08-05(大规模点映)"); only the first
    // 4-digit year is meaningful inside a pill.
    val displayYear = remember(detail.year) { detail.year.firstYearOrEmpty() }
    val displayRegion = remember(detail.region) { detail.region.trim() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // ---- Hero ----
        Box(
            // heightIn(min=...) instead of height(...): expanding the synopsis
            // must never clip the text, so the hero is allowed to grow with
            // its copy while keeping the 520dp minimum of the design.
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = BannerDimensions.heroHeight)
        ) {
            AsyncImage(
                model = backdrop,
                contentDescription = detail.title,
                contentScale = ContentScale.Crop,
                placeholder = ColorPainter(DaiflixColors.DarkSurface),
                error = ColorPainter(DaiflixColors.DarkCard),
                // matchParentSize(): the backdrop must follow the hero's
                // measured height, not drive it (the hero can grow when the
                // synopsis is expanded).
                modifier = Modifier.matchParentSize()
            )
            HorizontalScrim(modifier = Modifier.matchParentSize())
            // P3-3: the fade ends on the page background instead of opaque
            // black, so the bottom edge of the hero blends into the scrolling
            // content and leaves no hard horizontal seam at the screen bottom.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.55f to DaiflixColors.Black.copy(alpha = 0.45f),
                            1f to DaiflixColors.Background
                        )
                    )
            )
            TopScrim(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .align(Alignment.TopCenter)
            )

            // Shared back button (focus styling, white fill + black arrow on
            // focus, lives in Common.kt). DOWN is routed explicitly to the
            // play button because the poster column sits between them.
            DaiflixBackButton(
                onBack = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = DaiflixSpacing.screenPadding, top = DaiflixSpacing.md)
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown) {
                            // Swallow DOWN only when focus actually moved;
                            // otherwise fall back to default focus search so
                            // focus never gets stuck on the back button.
                            runCatching { playFocusRequester.requestFocus() }.isSuccess
                        } else {
                            false
                        }
                    },
                focusRequester = backFocusRequester
            )

            // Poster + copy at the bottom of the artwork.
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(
                        start = DaiflixSpacing.screenPadding,
                        // Clears the floating back button when the synopsis is
                        // expanded and the hero grows past one screen.
                        top = DaiflixSpacing.xxl + DaiflixSpacing.xl,
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
                        if (displayYear.isNotEmpty()) MetaPill(displayYear)
                        if (displayRegion.isNotEmpty()) MetaPill(displayRegion)
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
                            maxLines = if (descriptionExpanded) Int.MAX_VALUE else 3,
                            overflow = if (descriptionExpanded) {
                                TextOverflow.Clip
                            } else {
                                TextOverflow.Ellipsis
                            },
                            onTextLayout = { layout ->
                                // Only meaningful while clamped to 3 lines.
                                if (!descriptionExpanded) {
                                    descriptionOverflows = layout.hasVisualOverflow
                                }
                            }
                        )
                        if (descriptionOverflows || descriptionExpanded) {
                            Spacer(modifier = Modifier.height(DaiflixSpacing.xxs))
                            ExpandToggle(
                                expanded = descriptionExpanded,
                                onClick = { descriptionExpanded = !descriptionExpanded },
                                modifier = Modifier.focusRequester(expandFocusRequester),
                                playFocusRequester = playFocusRequester
                            )
                        }
                    }

                    // P2-3: director / cast were parsed but never shown. One
                    // compact line, no focus, so it never disturbs the D-pad
                    // chain between the play button and the back button.
                    val credits = listOfNotNull(
                        detail.director.cleanPeople().takeIf { it.isNotEmpty() }?.let { "导演：$it" },
                        detail.actors.cleanPeople().takeIf { it.isNotEmpty() }?.let { "主演：$it" }
                    ).joinToString("｜")
                    if (credits.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(DaiflixSpacing.xs))
                        Text(
                            text = credits,
                            style = MaterialTheme.typography.bodySmall,
                            color = DaiflixColors.LightGray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(DaiflixSpacing.md))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(DaiflixSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.onPreviewKeyEvent { event ->
                            // P2-4: reach the 展开/收起 toggle from the action row.
                            if (event.type == KeyEventType.KeyDown &&
                                event.key == Key.DirectionUp &&
                                (descriptionOverflows || descriptionExpanded)
                            ) {
                                runCatching { expandFocusRequester.requestFocus() }.isSuccess
                            } else {
                                false
                            }
                        }
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

/** Small focusable 展开/收起 chip shown only when the synopsis overflows. */
@Composable
private fun ExpandToggle(
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    playFocusRequester: FocusRequester
) {
    val focus = rememberFocusState()
    Surface(
        onClick = onClick,
        modifier = modifier
            .trackFocus(focus)
            .focusHalo(focus, cornerRadius = 6.dp)
            .onPreviewKeyEvent { event ->
                // DOWN goes back to the play button (the toggle sits above it).
                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown) {
                    runCatching { playFocusRequester.requestFocus() }.isSuccess
                } else {
                    false
                }
            },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(6.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = DaiflixColors.DarkCard,
            contentColor = DaiflixColors.LightGray,
            focusedContainerColor = DaiflixColors.White,
            focusedContentColor = DaiflixColors.Black,
            pressedContainerColor = DaiflixColors.DarkRed,
            pressedContentColor = DaiflixColors.White
        ),
        border = ClickableSurfaceDefaults.border(
            border = if (focus.isFocused) focusBorder() else Border.None
        )
    ) {
        Text(
            text = if (expanded) "收起" else "展开",
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            modifier = Modifier.padding(
                horizontal = DaiflixSpacing.md,
                vertical = DaiflixSpacing.xxs
            )
        )
    }
}

private val YEAR_REGEX = Regex("\\d{4}")

/** First 4-digit year in [this], or "" when there is none (audit P2-1). */
private fun String.firstYearOrEmpty(): String = YEAR_REGEX.find(this)?.value.orEmpty()

private val PEOPLE_SEPARATORS = Regex("[/、,，|｜;；\\s]+")

/**
 * The site stores credits as `/郭帆/` or `/吴京/刘德华/...`; strip the padding slashes
 * and normalise the separator so the line reads `导演：郭帆｜主演：吴京、刘德华`（P2-3）。
 */
private fun String.cleanPeople(): String = split(PEOPLE_SEPARATORS)
    .map { it.trim() }
    .filter { it.isNotEmpty() }
    .joinToString("、")

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
