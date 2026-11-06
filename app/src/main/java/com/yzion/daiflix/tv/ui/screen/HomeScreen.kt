package com.yzion.daiflix.tv.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yzion.daiflix.tv.data.model.Category
import com.yzion.daiflix.tv.data.model.VodItem
import com.yzion.daiflix.tv.ui.component.EmptyState
import com.yzion.daiflix.tv.ui.component.HeroSection
import com.yzion.daiflix.tv.ui.component.LoadingState
import com.yzion.daiflix.tv.ui.component.TopNavBar
import com.yzion.daiflix.tv.ui.component.VodRow
import com.yzion.daiflix.tv.ui.theme.BannerDimensions
import com.yzion.daiflix.tv.ui.theme.DaiflixColors
import com.yzion.daiflix.tv.ui.theme.DaiflixSpacing
import com.yzion.daiflix.tv.ui.theme.NavigationDimensions
import com.yzion.daiflix.tv.ui.viewmodel.HomeUiState
import kotlinx.coroutines.launch
import com.yzion.daiflix.tv.ui.viewmodel.HomeViewModel

/**
 * Netflix-style Home.
 *
 * A full-bleed, auto-rotating hero sits behind the transparent top navigation;
 * every carousel below is a [VodRow] with focus-driven horizontal scrolling. The
 * screen owns no scrolling of its own besides the vertical [LazyColumn].
 */
@Composable
fun HomeScreen(
    onVodClick: (String) -> Unit,
    onPlayClick: (String) -> Unit,
    onCategoryClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Compose for TV parks no focus for us, and requesting focus on a control that
    // lives inside the LazyColumn makes the list scroll the hero out of frame. Anchor
    // the initial focus on the navigation bar (always laid out) and hand DOWN to the
    // hero actions explicitly, which keeps the hero fully visible on entry.
    val navFocus = remember { FocusRequester() }
    val heroPlayFocus = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val firstRowFocus = remember { FocusRequester() }
    var heroFocused by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading) {
            repeat(4) {
                delay(120)
                runCatching { navFocus.requestFocus() }
            }
        }
    }

    val hasContent = uiState.banners.isNotEmpty() ||
        uiState.rows.isNotEmpty() ||
        uiState.watchHistory.isNotEmpty()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DaiflixColors.Background)
            // A focused tv Surface swallows arrow keys, so the hero -> rows handoff is
            // intercepted here (preview pass) instead of inside the hero composable.
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionDown -> if (heroFocused) {
                        heroFocused = false
                        runCatching { firstRowFocus.requestFocus() }
                        true
                    } else {
                        false
                    }

                    Key.DirectionUp -> if (!heroFocused &&
                        (listState.firstVisibleItemIndex > 0 ||
                            listState.firstVisibleItemScrollOffset > 160)
                    ) {
                        scope.launch {
                            listState.animateScrollToItem(0)
                            delay(180)
                            runCatching { heroPlayFocus.requestFocus() }
                            heroFocused = true
                        }
                        true
                    } else {
                        false
                    }

                    else -> false
                }
            }
    ) {
        when {
            uiState.isLoading && !hasContent -> {
                LoadingState(message = "正在加载首页…")
            }

            uiState.error != null && !hasContent -> {
                EmptyState(
                    title = "加载失败",
                    message = uiState.error,
                    actionLabel = "重试",
                    onAction = { viewModel.refresh() }
                )
            }

            else -> {
                HomeContent(
                    uiState = uiState,
                    onVodClick = onVodClick,
                    onPlayClick = onPlayClick,
                    heroPlayFocus = heroPlayFocus,
                    navFocus = navFocus,
                    firstRowFocus = firstRowFocus,
                    onHeroFocusChange = { heroFocused = it },
                    listState = listState
                )
            }
        }

        // Drawn last so the hero artwork scrolls underneath the transparent bar.
        TopNavBar(
            selectedCategory = Category.HOME,
            onCategoryClick = { category ->
                if (category == Category.HOME) viewModel.refresh() else onCategoryClick(category.value)
            },
            onSearchClick = onSearchClick,
            onSettingsClick = onSettingsClick,
            onHistoryClick = onHistoryClick,
            onFavoritesClick = onFavoritesClick,
            modifier = Modifier.align(Alignment.TopCenter),
            transparent = true,
            firstTabFocusRequester = navFocus,
            onNavigateDown = {
                // The nav always hands DOWN to the hero; if the rows were scrolled
                // (hero covered), bring the page back to the top first.
                scope.launch {
                    if (listState.firstVisibleItemIndex > 0 ||
                        listState.firstVisibleItemScrollOffset > 40
                    ) {
                        listState.animateScrollToItem(0)
                        delay(180)
                    }
                    runCatching { heroPlayFocus.requestFocus() }
                }
            }
        )
    }
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    onVodClick: (String) -> Unit,
    onPlayClick: (String) -> Unit,
    heroPlayFocus: FocusRequester,
    navFocus: FocusRequester,
    firstRowFocus: FocusRequester,
    onHeroFocusChange: (Boolean) -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    // WatchHistory is not a VodItem, so map it once for the "继续观看" carousel.
    val continueWatching = remember(uiState.watchHistory) {
        uiState.watchHistory.map { history ->
            VodItem(
                id = history.vodId,
                title = history.title,
                coverUrl = history.coverUrl,
                category = Category.HOME
            )
        }
    }
    val progressById = remember(uiState.watchHistory) {
        uiState.watchHistory
            .filter { it.duration > 0L && it.position > 0L }
            .associate { it.vodId to it.progress }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // A little taller than the token so the copy breathes, but never taller
        // than the viewport (safe on 720p panels / density overrides).
        val heroHeight = minOf(
            BannerDimensions.heroHeight + DaiflixSpacing.xxl,
            maxHeight * 0.74f
        )
        // With no hero the nav is the only thing above the first row.
        val contentTop = if (uiState.banners.isEmpty()) {
            NavigationDimensions.topNavHeight + DaiflixSpacing.lg
        } else {
            heroHeight - DaiflixSpacing.xxl
        }

        // The hero is fixed behind the list: rows slide over it, so focusing the Play
        // button never scrolls the artwork behind the transparent nav bar. It fades out
        // as the rows come up so copy and posters never overlap.
        val heroAlpha = if (listState.firstVisibleItemIndex > 0) {
            0f
        } else {
            (1f - listState.firstVisibleItemScrollOffset / 320f).coerceIn(0f, 1f)
        }
        if (uiState.banners.isNotEmpty() && heroAlpha > 0.01f) {
            HeroSection(
                banners = uiState.banners,
                onVodClick = onVodClick,
                onPlayClick = onPlayClick,
                height = heroHeight,
                modifier = Modifier.align(Alignment.TopCenter),
                playFocusRequester = heroPlayFocus,
                onNavigateUp = { runCatching { navFocus.requestFocus() } },
                onNavigateDown = { runCatching { firstRowFocus.requestFocus() } },
                onPlayFocusChange = onHeroFocusChange,
                visibilityAlpha = heroAlpha
            )
        }

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                top = contentTop,
                bottom = DaiflixSpacing.xxxl
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            if (continueWatching.isNotEmpty()) {
                item(key = "home-continue-watching") {
                    VodRow(
                        title = "继续观看",
                        items = continueWatching,
                        onVodClick = onVodClick,
                        modifier = Modifier.padding(top = DaiflixSpacing.rowGap),
                        progressById = progressById,
                        firstItemFocusRequester = firstRowFocus
                    )
                }
            }

            itemsIndexed(
                items = uiState.rows,
                key = { index, _ -> "home-row-$index" }
            ) { index, row ->
                VodRow(
                    title = row.title,
                    items = row.items,
                    onVodClick = onVodClick,
                    modifier = Modifier.padding(top = DaiflixSpacing.rowGap),
                    firstItemFocusRequester = if (index == 0 && continueWatching.isEmpty()) {
                        firstRowFocus
                    } else {
                        null
                    }
                )
            }
        }
    }
}
