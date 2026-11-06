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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.yzion.daiflix.tv.data.model.VodItem
import com.yzion.daiflix.tv.ui.component.EmptyState
import com.yzion.daiflix.tv.ui.component.LoadingState
import com.yzion.daiflix.tv.ui.component.focusBorder
import com.yzion.daiflix.tv.ui.theme.DaiflixColors
import com.yzion.daiflix.tv.ui.theme.DaiflixSpacing
import com.yzion.daiflix.tv.ui.viewmodel.SearchViewModel

/**
 * TV search: big query display, on-screen D-pad keyboard, poster grid results and
 * focusable history chips.
 */
@Composable
fun SearchScreen(
    onVodClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    BackHandler(onBack = onBack)

    val firstKeyFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        runCatching { firstKeyFocusRequester.requestFocus() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DaiflixColors.Background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SearchHeader(query = state.query, onBack = onBack)

            OnScreenKeyboard(
                onLetter = { char ->
                    viewModel.updateQuery(state.query + char.lowercase())
                    // search() debounces internally, so live suggestions stay cheap.
                    viewModel.search()
                },
                onSpace = {
                    viewModel.updateQuery(state.query + " ")
                    viewModel.search()
                },
                onBackspace = {
                    if (state.query.isNotEmpty()) {
                        val next = state.query.dropLast(1)
                        if (next.isBlank()) {
                            viewModel.clearResults()
                        } else {
                            viewModel.updateQuery(next)
                            viewModel.search()
                        }
                    }
                },
                onClear = { viewModel.clearResults() },
                onSearch = { viewModel.search() },
                firstKeyFocusRequester = firstKeyFocusRequester
            )

            Spacer(modifier = Modifier.height(DaiflixSpacing.md))

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    state.isSearching && state.results.isEmpty() -> {
                        LoadingState(message = "搜索中…")
                    }
                    state.results.isNotEmpty() -> {
                        SearchResults(
                            results = state.results,
                            onVodClick = onVodClick
                        )
                    }
                    state.query.isNotBlank() -> {
                        EmptyState(
                            title = "未找到相关内容",
                            message = state.error ?: "换个关键词试试，支持中文与拼音",
                            actionLabel = "清空",
                            onAction = { viewModel.clearResults() }
                        )
                    }
                    state.searchHistory.isNotEmpty() -> {
                        SearchHistorySection(
                            history = state.searchHistory.map { it.keyword },
                            onKeywordClick = { keyword ->
                                viewModel.updateQuery(keyword)
                                viewModel.search(keyword)
                            },
                            onClearHistory = { viewModel.clearHistory() }
                        )
                    }
                    else -> {
                        EmptyState(
                            title = "输入片名开始搜索",
                            message = "支持中文片名与拼音输入，例如 liulang"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchHeader(
    query: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = DaiflixSpacing.screenPadding,
                end = DaiflixSpacing.screenPadding,
                top = DaiflixSpacing.lg,
                bottom = DaiflixSpacing.md
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            onClick = onBack,
            modifier = Modifier.size(48.dp),
            shape = ClickableSurfaceDefaults.shape(CircleShape),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = DaiflixColors.DarkSurface,
                contentColor = DaiflixColors.White,
                focusedContainerColor = DaiflixColors.White,
                focusedContentColor = DaiflixColors.Black
            )
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "返回",
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(DaiflixSpacing.lg))

        Box(
            modifier = Modifier
                .weight(1f)
                .height(84.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(DaiflixColors.DarkSurface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = DaiflixSpacing.lg),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (query.isBlank()) {
                    Text(
                        text = "输入片名开始搜索",
                        style = MaterialTheme.typography.headlineLarge,
                        color = DaiflixColors.MediumGray,
                        maxLines = 1
                    )
                } else {
                    Text(
                        text = query,
                        style = MaterialTheme.typography.headlineLarge,
                        color = DaiflixColors.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                Text(
                    text = "▏",
                    style = MaterialTheme.typography.headlineLarge,
                    color = DaiflixColors.Red
                )
            }
        }

        Spacer(modifier = Modifier.width(DaiflixSpacing.lg))

        Text(
            text = "按「搜索」确认",
            style = MaterialTheme.typography.labelMedium,
            color = DaiflixColors.MediumGray
        )
    }
}

@Composable
private fun OnScreenKeyboard(
    onLetter: (String) -> Unit,
    onSpace: () -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onSearch: () -> Unit,
    firstKeyFocusRequester: FocusRequester
) {
    val rows = listOf(
        listOf("q", "w", "e", "r", "t", "y"),
        listOf("u", "i", "o", "p", "a", "s"),
        listOf("d", "f", "g", "h", "j", "k"),
        listOf("l", "z", "x", "c", "v", "b")
    )
    val bottomRow = listOf("n", "m", "空格", "删除", "清空", "搜索")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DaiflixSpacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(DaiflixSpacing.sm)
    ) {
        rows.forEachIndexed { rowIndex, keys ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DaiflixSpacing.sm)
            ) {
                keys.forEachIndexed { keyIndex, key ->
                    KeyboardKey(
                        label = key,
                        modifier = Modifier.weight(1f),
                        focusRequester = if (rowIndex == 0 && keyIndex == 0) {
                            firstKeyFocusRequester
                        } else {
                            null
                        },
                        onClick = { onLetter(key) }
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DaiflixSpacing.sm)
        ) {
            bottomRow.forEachIndexed { keyIndex, key ->
                val primary = key == "搜索"
                KeyboardKey(
                    label = key,
                    modifier = Modifier.weight(1f),
                    primary = primary,
                    onClick = {
                        when (key) {
                            "空格" -> onSpace()
                            "删除" -> onBackspace()
                            "清空" -> onClear()
                            "搜索" -> onSearch()
                            else -> onLetter(key)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun KeyboardKey(
    label: String,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit
) {
    val keyModifier = if (focusRequester != null) {
        modifier.focusRequester(focusRequester)
    } else {
        modifier
    }

    Surface(
        onClick = onClick,
        modifier = keyModifier.height(64.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (primary) DaiflixColors.Red else DaiflixColors.DarkCard,
            contentColor = DaiflixColors.White,
            focusedContainerColor = DaiflixColors.White,
            focusedContentColor = DaiflixColors.Black
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
        border = ClickableSurfaceDefaults.border(focusedBorder = focusBorder())
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SearchResults(
    results: List<VodItem>,
    onVodClick: (String) -> Unit
) {
    val spacing = DaiflixSpacing.md
    val config = LocalConfiguration.current
    val cardWidth = ((config.screenWidthDp - 2 * DaiflixSpacing.screenPadding.value - 5 * spacing.value) / 6)
        .coerceAtLeast(120f)
        .dp

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "搜索结果（${results.size}）",
            style = MaterialTheme.typography.titleMedium,
            color = DaiflixColors.LightGray,
            modifier = Modifier.padding(
                start = DaiflixSpacing.screenPadding,
                end = DaiflixSpacing.screenPadding,
                bottom = DaiflixSpacing.sm
            )
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(
                start = DaiflixSpacing.screenPadding,
                end = DaiflixSpacing.screenPadding,
                bottom = DaiflixSpacing.xxxl
            ),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalArrangement = Arrangement.spacedBy(DaiflixSpacing.lg)
        ) {
            items(results, key = { it.id }) { item ->
                SearchPosterCard(
                    item = item,
                    width = cardWidth,
                    onClick = { onVodClick(item.id) }
                )
            }
        }
    }
}

@Composable
private fun SearchPosterCard(
    item: VodItem,
    width: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.width(width),
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
                if (item.rating.isNotBlank()) {
                    Text(
                        text = item.rating,
                        style = MaterialTheme.typography.labelSmall,
                        color = DaiflixColors.Gold,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(DaiflixSpacing.xs)
                    )
                }
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
                    top = DaiflixSpacing.xs,
                    bottom = DaiflixSpacing.sm
                )
            )
        }
    }
}

@Composable
private fun SearchHistorySection(
    history: List<String>,
    onKeywordClick: (String) -> Unit,
    onClearHistory: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DaiflixSpacing.screenPadding)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "搜索历史",
                style = MaterialTheme.typography.titleMedium,
                color = DaiflixColors.White
            )
            Surface(
                onClick = onClearHistory,
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(6.dp)),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = DaiflixColors.DarkSurface,
                    contentColor = DaiflixColors.LightGray,
                    focusedContainerColor = DaiflixColors.White,
                    focusedContentColor = DaiflixColors.Black
                ),
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
                border = ClickableSurfaceDefaults.border(focusedBorder = focusBorder())
            ) {
                Text(
                    text = "清除历史",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(
                        horizontal = DaiflixSpacing.md,
                        vertical = DaiflixSpacing.xs
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(DaiflixSpacing.md))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(DaiflixSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(count = history.size) { index ->
                val keyword = history[index]
                Surface(
                    onClick = { onKeywordClick(keyword) },
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(6.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = DaiflixColors.DarkCard,
                        contentColor = DaiflixColors.LightGray,
                        focusedContainerColor = DaiflixColors.White,
                        focusedContentColor = DaiflixColors.Black
                    ),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
                    border = ClickableSurfaceDefaults.border(focusedBorder = focusBorder())
                ) {
                    Text(
                        text = keyword,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        modifier = Modifier.padding(
                            horizontal = DaiflixSpacing.lg,
                            vertical = DaiflixSpacing.sm
                        )
                    )
                }
            }
        }
    }
}
