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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
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
import com.yzion.daiflix.tv.ui.component.RequestFocusWhenReady
import com.yzion.daiflix.tv.ui.component.focusHalo
import com.yzion.daiflix.tv.ui.component.insetFocusBorder
import com.yzion.daiflix.tv.ui.component.rememberFocusState
import com.yzion.daiflix.tv.ui.component.trackFocus
import com.yzion.daiflix.tv.ui.theme.DaiflixColors
import com.yzion.daiflix.tv.ui.theme.DaiflixSpacing
import com.yzion.daiflix.tv.ui.viewmodel.SearchViewModel

/** 键盘键高，审计要求 48–56dp。 */
private val KeyboardKeyHeight = 52.dp

/** 键间距。 */
private val KeyboardKeyGap = 8.dp

/** 右侧键盘占屏宽的比例与上限（审计建议：约 40% 屏宽，上限 560dp）。 */
private const val KeyboardWidthFraction = 0.4f
private const val KeyboardMaxWidthDp = 560f

/** 结果卡片最小宽度，两栏布局下 960x540dp 屏约 3 列。 */
private val ResultCardMinWidth = 132.dp

/**
 * TV 搜索页（审计 P0-2 修复）。
 *
 * 旧实现是「Header + 全宽键盘 + 结果区」的纵向堆叠，键盘吃掉约 700px，结果区只剩
 * 约 70px，且 DPAD_DOWN 无法从键盘移进结果区。现在改成两栏：
 *
 * ```
 * ┌──────────────────────────────────────────────┐
 * │ 返回  查询框（全宽）                 提示     │  Header
 * ├───────────────────────────┬──────────────────┤
 * │ 结果区（约 60% 宽，可滚动）│ 键盘（约 40%，    │
 * │  · 搜索结果网格           │  4 行字母 +      │
 * │  · 空态 / 搜索历史        │  1 行功能键）    │
 * └───────────────────────────┴──────────────────┘
 * ```
 *
 * 焦点链显式接线（tv Surface 会吞方向键，不能只靠几何 focus search）：
 * - 键盘最底行 DOWN → 结果区第一张卡（结果为空时吃掉事件，焦点留在键盘）；
 * - 结果区最顶行 UP → 键盘最底行的「空格」键；
 * - 提交搜索（「搜索」键 / 历史 chip）且结果非空 → 焦点自动移到第一张卡。
 *
 * 所有焦点样式都由 [rememberFocusState] + [trackFocus] 显式驱动；不用
 * `collectIsFocusedAsState()`，因为它的 interaction Flow 不重放事件，首屏抢焦点
 * 时第一个 Focus 事件会丢，出现「有焦点但没样式，按一下方向键才亮」。
 */
@Composable
fun SearchScreen(
    onVodClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    BackHandler(onBack = onBack)

    // 键盘宽度按屏宽比例算，并在 TV 常见 960dp 宽的屏上封顶 560dp。
    val keyboardWidth = (LocalConfiguration.current.screenWidthDp * KeyboardWidthFraction)
        .coerceAtMost(KeyboardMaxWidthDp)
        .dp

    val firstKeyFocusRequester = remember { FocusRequester() }
    val keyboardBottomFocusRequester = remember { FocusRequester() }
    val firstResultFocusRequester = remember { FocusRequester() }

    // 首屏焦点：键盘第一行第一个键（q）。
    RequestFocusWhenReady(firstKeyFocusRequester)

    // 只有「显式提交」才把焦点交给结果区；输入即搜索（debounce）的实时结果不抢焦点。
    var pendingResultFocus by remember { mutableStateOf(false) }
    var resultFocusSignal by remember { mutableIntStateOf(0) }

    LaunchedEffect(state.results, state.isSearching, pendingResultFocus) {
        if (pendingResultFocus && !state.isSearching && state.results.isNotEmpty()) {
            pendingResultFocus = false
            resultFocusSignal++
        }
    }
    RequestFocusWhenReady(
        focusRequester = firstResultFocusRequester,
        key = resultFocusSignal,
        enabled = resultFocusSignal > 0
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DaiflixColors.Background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SearchHeader(query = state.query, onBack = onBack)

            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = DaiflixSpacing.screenPadding)
            ) {
                // 左栏：结果区（网格 / 搜索中 / 空态 / 搜索历史）。
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    when {
                        state.isSearching && state.results.isEmpty() -> {
                            LoadingState(message = "搜索中…")
                        }

                        state.results.isNotEmpty() -> {
                            SearchResults(
                                results = state.results,
                                onVodClick = onVodClick,
                                firstCardFocusRequester = firstResultFocusRequester,
                                onNavigateUpToKeyboard = {
                                    runCatching {
                                        keyboardBottomFocusRequester.requestFocus()
                                    }.isSuccess
                                }
                            )
                        }

                        state.query.isNotBlank() -> {
                            EmptyState(
                                title = "未找到相关内容",
                                message = state.error ?: "换个关键词试试，支持中文与拼音",
                                actionLabel = "清空",
                                onAction = {
                                    pendingResultFocus = false
                                    viewModel.clearResults()
                                }
                            )
                        }

                        state.searchHistory.isNotEmpty() -> {
                            SearchHistorySection(
                                history = state.searchHistory.map { it.keyword },
                                onKeywordClick = { keyword ->
                                    // 点历史 chip 等于一次显式提交，结果回来后焦点交给结果区。
                                    pendingResultFocus = true
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

                Spacer(modifier = Modifier.width(DaiflixSpacing.lg))

                // 右栏：屏幕键盘。垂直居中，避免顶部留白全堆在下方。
                Box(
                    modifier = Modifier
                        .width(keyboardWidth)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    OnScreenKeyboard(
                        onLetter = { char ->
                            pendingResultFocus = false
                            viewModel.updateQuery(state.query + char.lowercase())
                            // search() 内部有 debounce，输入即搜索不会打爆接口。
                            viewModel.search()
                        },
                        onSpace = {
                            pendingResultFocus = false
                            viewModel.updateQuery(state.query + " ")
                            viewModel.search()
                        },
                        onBackspace = {
                            pendingResultFocus = false
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
                        onClear = {
                            pendingResultFocus = false
                            viewModel.clearResults()
                        },
                        onSearch = {
                            pendingResultFocus = true
                            viewModel.search()
                        },
                        firstKeyFocusRequester = firstKeyFocusRequester,
                        bottomRowFocusRequester = keyboardBottomFocusRequester,
                        resultsAvailable = state.results.isNotEmpty(),
                        onNavigateDownToResults = {
                            runCatching { firstResultFocusRequester.requestFocus() }.isSuccess
                        }
                    )
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
    val backFocus = rememberFocusState()

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
            modifier = Modifier
                .size(48.dp)
                .trackFocus(backFocus)
                .focusHalo(backFocus, cornerRadius = 24.dp),
            shape = ClickableSurfaceDefaults.shape(CircleShape),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = if (backFocus.isFocused) {
                    DaiflixColors.White
                } else {
                    DaiflixColors.DarkSurface
                },
                contentColor = if (backFocus.isFocused) {
                    DaiflixColors.Black
                } else {
                    DaiflixColors.White
                },
                focusedContainerColor = if (backFocus.isFocused) {
                    DaiflixColors.White
                } else {
                    DaiflixColors.DarkSurface
                },
                focusedContentColor = if (backFocus.isFocused) {
                    DaiflixColors.Black
                } else {
                    DaiflixColors.White
                }
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
    firstKeyFocusRequester: FocusRequester,
    bottomRowFocusRequester: FocusRequester,
    resultsAvailable: Boolean,
    onNavigateDownToResults: () -> Boolean
) {
    val rows = listOf(
        listOf("q", "w", "e", "r", "t", "y"),
        listOf("u", "i", "o", "p", "a", "s"),
        listOf("d", "f", "g", "h", "j", "k"),
        listOf("l", "z", "x", "c", "v", "b")
    )
    val bottomRow = listOf("n", "m", "空格", "删除", "清空", "搜索")

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(KeyboardKeyGap)
    ) {
        rows.forEachIndexed { rowIndex, keys ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(KeyboardKeyGap)
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

        // 最底行：DOWN 显式移交给结果区第一张卡；结果为空时吃掉事件，焦点留在键盘。
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown || event.key != Key.DirectionDown) {
                        return@onPreviewKeyEvent false
                    }
                    if (resultsAvailable) onNavigateDownToResults() else true
                },
            horizontalArrangement = Arrangement.spacedBy(KeyboardKeyGap)
        ) {
            bottomRow.forEach { key ->
                KeyboardKey(
                    label = key,
                    modifier = Modifier.weight(1f),
                    primary = key == "搜索",
                    focusRequester = if (key == "空格") bottomRowFocusRequester else null,
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
    val focus = rememberFocusState()
    val containerColor = when {
        focus.isFocused -> DaiflixColors.White
        primary -> DaiflixColors.Red
        else -> DaiflixColors.DarkCard
    }
    val contentColor = if (focus.isFocused) DaiflixColors.Black else DaiflixColors.White

    val keyModifier = if (focusRequester != null) {
        modifier.focusRequester(focusRequester)
    } else {
        modifier
    }

    Surface(
        onClick = onClick,
        modifier = keyModifier
            .height(KeyboardKeyHeight)
            .trackFocus(focus)
            .focusHalo(focus, cornerRadius = 8.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = containerColor,
            contentColor = contentColor,
            focusedContainerColor = containerColor,
            focusedContentColor = contentColor
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = label.uppercase(),
                style = if (label.length > 1) {
                    MaterialTheme.typography.labelLarge
                } else {
                    MaterialTheme.typography.titleMedium
                },
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SearchResults(
    results: List<VodItem>,
    onVodClick: (String) -> Unit,
    firstCardFocusRequester: FocusRequester,
    onNavigateUpToKeyboard: () -> Boolean
) {
    val gridState = rememberLazyGridState()
    var focusedIndex by remember { mutableStateOf(-1) }

    // 结果集变化（新一次搜索）后回到顶部，保证第一张卡已组合、可接管焦点。
    LaunchedEffect(results) {
        focusedIndex = -1
        if (gridState.firstVisibleItemIndex != 0 || gridState.firstVisibleItemScrollOffset != 0) {
            gridState.scrollToItem(0)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .onFocusChanged { if (!it.hasFocus) focusedIndex = -1 }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || event.key != Key.DirectionUp) {
                    return@onPreviewKeyEvent false
                }
                val layoutInfo = gridState.layoutInfo
                val topRowOffset = layoutInfo.visibleItemsInfo.minOfOrNull { it.offset.y }
                val focusedItem = layoutInfo.visibleItemsInfo.firstOrNull { it.index == focusedIndex }
                val onTopRow = topRowOffset != null &&
                    focusedItem != null &&
                    focusedItem.offset.y <= topRowOffset
                // 仅在「焦点已经在最顶行且网格没有向上滚动空间」时把 UP 移交给键盘，
                // 其余情况保持 LazyGrid 的默认焦点移动/滚动。
                if (focusedIndex >= 0 && onTopRow && !gridState.canScrollBackward) {
                    onNavigateUpToKeyboard()
                } else {
                    false
                }
            }
    ) {
        Text(
            text = "搜索结果（${results.size}）",
            style = MaterialTheme.typography.titleMedium,
            color = DaiflixColors.LightGray,
            modifier = Modifier.padding(
                start = DaiflixSpacing.xxs,
                end = DaiflixSpacing.xxs,
                bottom = DaiflixSpacing.sm
            )
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = ResultCardMinWidth),
            state = gridState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(
                start = DaiflixSpacing.xxs,
                end = DaiflixSpacing.xxs,
                top = DaiflixSpacing.xs,
                bottom = DaiflixSpacing.xl
            ),
            horizontalArrangement = Arrangement.spacedBy(DaiflixSpacing.md),
            verticalArrangement = Arrangement.spacedBy(DaiflixSpacing.md)
        ) {
            itemsIndexed(results, key = { _, item -> item.id }) { index, item ->
                SearchPosterCard(
                    item = item,
                    onClick = { onVodClick(item.id) },
                    onFocused = { focusedIndex = index },
                    modifier = if (index == 0) {
                        Modifier.focusRequester(firstCardFocusRequester)
                    } else {
                        Modifier
                    }
                )
            }
        }
    }
}

@Composable
private fun SearchPosterCard(
    item: VodItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFocused: () -> Unit = {}
) {
    val focus = rememberFocusState()

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .trackFocus(focus)
            .insetFocusBorder(focus)
            .onFocusChanged { if (it.isFocused) onFocused() },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = DaiflixColors.DarkCard,
            contentColor = DaiflixColors.White,
            focusedContainerColor = DaiflixColors.DarkCard,
            focusedContentColor = DaiflixColors.White
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
    val clearFocus = rememberFocusState()

    Column(modifier = Modifier.fillMaxWidth()) {
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
                modifier = Modifier.trackFocus(clearFocus),
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(6.dp)),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = if (clearFocus.isFocused) {
                        DaiflixColors.White
                    } else {
                        DaiflixColors.DarkSurface
                    },
                    contentColor = if (clearFocus.isFocused) {
                        DaiflixColors.Black
                    } else {
                        DaiflixColors.LightGray
                    },
                    focusedContainerColor = if (clearFocus.isFocused) {
                        DaiflixColors.White
                    } else {
                        DaiflixColors.DarkSurface
                    },
                    focusedContentColor = if (clearFocus.isFocused) {
                        DaiflixColors.Black
                    } else {
                        DaiflixColors.LightGray
                    }
                )
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
                val chipFocus = rememberFocusState()
                Surface(
                    onClick = { onKeywordClick(keyword) },
                    modifier = Modifier.trackFocus(chipFocus),
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(6.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = if (chipFocus.isFocused) {
                            DaiflixColors.White
                        } else {
                            DaiflixColors.DarkCard
                        },
                        contentColor = if (chipFocus.isFocused) {
                            DaiflixColors.Black
                        } else {
                            DaiflixColors.LightGray
                        },
                        focusedContainerColor = if (chipFocus.isFocused) {
                            DaiflixColors.White
                        } else {
                            DaiflixColors.DarkCard
                        },
                        focusedContentColor = if (chipFocus.isFocused) {
                            DaiflixColors.Black
                        } else {
                            DaiflixColors.LightGray
                        }
                    )
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
