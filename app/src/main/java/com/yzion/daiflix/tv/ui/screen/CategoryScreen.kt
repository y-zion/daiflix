package com.yzion.daiflix.tv.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.yzion.daiflix.tv.data.model.Category
import com.yzion.daiflix.tv.data.model.VodItem
import com.yzion.daiflix.tv.ui.component.CategoryFilterBar
import com.yzion.daiflix.tv.ui.component.DaiflixPrimaryButton
import com.yzion.daiflix.tv.ui.component.DaiflixSecondaryButton
import com.yzion.daiflix.tv.ui.component.EmptyState
import com.yzion.daiflix.tv.ui.component.LoadingState
import com.yzion.daiflix.tv.ui.component.RequestFocusWhenReady
import com.yzion.daiflix.tv.ui.component.TopNavBar
import com.yzion.daiflix.tv.ui.component.VodCard
import com.yzion.daiflix.tv.ui.theme.DaiflixColors
import com.yzion.daiflix.tv.ui.theme.DaiflixSpacing
import com.yzion.daiflix.tv.ui.viewmodel.CategorySort
import com.yzion.daiflix.tv.ui.viewmodel.CategoryUiState
import com.yzion.daiflix.tv.ui.viewmodel.CategoryViewModel

/** Six posters per row: two full rows + a peek of the third fit a 16:9 TV. */
private const val GRID_COLUMNS = 6

/**
 * Category browse: a dense, focus-scrollable poster grid under the solid
 * navigation bar, with a title/count header and proper loading/error/empty states.
 *
 * 审计 P2-8：488 部影片只能一路滚动 —— 标题下方加客户端筛选条（年份 + 排序），
 * 只用已加载数据做本地过滤/排序（不重新请求、不闪 loading），并显示
 * 「共 N 部 · 已筛选 M 部」计数与筛选空态。
 */
@Composable
fun CategoryScreen(
    categoryId: String,
    onVodClick: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onBack: () -> Unit,
    viewModel: CategoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val category = remember(categoryId) { Category.fromValue(categoryId) }
    val gridState = rememberLazyGridState()
    val firstCardFocusRequester = remember { FocusRequester() }
    // 网格首行按「上」时显式聚焦的第一个筛选 chip（焦点条的落点）。
    val filterBarFocusRequester = remember { FocusRequester() }
    // 空态 / 错误态的「重试 / 返回首页」是页面上仅有的可聚焦控件，必须由它接管焦点。
    val emptyActionFocus = remember { FocusRequester() }

    // 网格首次出现（加载完成）或筛选条件变化导致结果重新非空后，聚焦第一张卡片：
    // 等一帧再请求，卡片自身的聚焦订阅已建立，焦点样式首帧即可见（审计 P1-3）。
    RequestFocusWhenReady(
        focusRequester = firstCardFocusRequester,
        key = uiState.visibleItems.isEmpty(),
        enabled = uiState.visibleItems.isNotEmpty()
    )

    LaunchedEffect(category) {
        viewModel.loadCategory(category)
        // A new category starts at the top instead of inheriting the old offset.
        gridState.scrollToItem(0)
    }

    // 筛选/排序只改变派生列表（本地操作，不会重新请求网络），列表变化后回到顶部，
    // 避免滚动位置停在越界的地方。
    LaunchedEffect(uiState.selectedYearId, uiState.sort) {
        gridState.scrollToItem(0)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DaiflixColors.Background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopNavBar(
                selectedCategory = category,
                onCategoryClick = { clicked ->
                    when {
                        clicked == Category.HOME -> onBack()
                        clicked != category -> onCategoryChange(clicked.value)
                        else -> Unit
                    }
                },
                onSearchClick = onSearchClick,
                onSettingsClick = onSettingsClick,
                onHistoryClick = onHistoryClick,
                onFavoritesClick = onFavoritesClick
            )

            when {
                uiState.isLoading && uiState.items.isEmpty() -> {
                    LoadingState(
                        modifier = Modifier.weight(1f),
                        message = "正在加载${category.displayName}…"
                    )
                }

                uiState.error != null && uiState.items.isEmpty() -> {
                    CategoryError(
                        message = uiState.error,
                        onRetry = { viewModel.loadCategory(category) },
                        onBack = onBack,
                        retryFocusRequester = emptyActionFocus,
                        modifier = Modifier.weight(1f)
                    )
                }

                uiState.items.isEmpty() -> {
                    EmptyState(
                        title = "暂无内容",
                        message = "该分类下暂时没有可播放的内容",
                        actionLabel = "返回首页",
                        onAction = onBack,
                        modifier = Modifier.weight(1f),
                        actionFocusRequester = emptyActionFocus
                    )
                }

                else -> {
                    CategoryContent(
                        category = category,
                        uiState = uiState,
                        gridState = gridState,
                        firstCardFocusRequester = firstCardFocusRequester,
                        filterBarFocusRequester = filterBarFocusRequester,
                        onYearSelected = viewModel::selectYear,
                        onSortSelected = viewModel::selectSort,
                        onResetFilters = viewModel::resetFilters,
                        onVodClick = onVodClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * 已加载内容区：标题 + 计数 + 筛选条 + （网格 或 筛选空态）。
 * 筛选条固定在网格上方、不随网格滚动，也不会盖住网格。
 */
@Composable
private fun CategoryContent(
    category: Category,
    uiState: CategoryUiState,
    gridState: LazyGridState,
    firstCardFocusRequester: FocusRequester,
    filterBarFocusRequester: FocusRequester,
    onYearSelected: (String) -> Unit,
    onSortSelected: (CategorySort) -> Unit,
    onResetFilters: () -> Unit,
    onVodClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val resetFilterFocusRequester = remember { FocusRequester() }

    Column(modifier = modifier.fillMaxSize()) {
        CategoryHeader(
            category = category,
            totalCount = uiState.totalCount,
            filteredCount = uiState.filteredCount
        )

        CategoryFilterBar(
            yearOptions = uiState.yearOptions,
            selectedYearId = uiState.selectedYearId,
            onYearSelected = onYearSelected,
            sort = uiState.sort,
            onSortSelected = onSortSelected,
            firstChipFocusRequester = filterBarFocusRequester,
            // 筛选空态时网格没有卡片可聚焦，requestFocus 会失败并返回 false，
            // 默认焦点搜索会把焦点交给空态里的「重置筛选」。
            onNavigateDown = { requestFocusOrFalse(firstCardFocusRequester) }
        )

        if (uiState.visibleItems.isEmpty()) {
            EmptyState(
                title = "该筛选下暂无内容",
                message = "当前筛选条件没有匹配的影片，重置筛选可查看全部 ${uiState.totalCount} 部",
                actionLabel = "重置筛选",
                onAction = onResetFilters,
                modifier = Modifier.weight(1f),
                actionFocusRequester = resetFilterFocusRequester
            )
        } else {
            CategoryGrid(
                items = uiState.visibleItems,
                gridState = gridState,
                firstItemFocusRequester = firstCardFocusRequester,
                onNavigateUpToFilterBar = { requestFocusOrFalse(filterBarFocusRequester) },
                onVodClick = onVodClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CategoryHeader(
    category: Category,
    totalCount: Int,
    filteredCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = DaiflixSpacing.screenPadding,
                end = DaiflixSpacing.screenPadding,
                top = DaiflixSpacing.lg,
                bottom = DaiflixSpacing.xs
            ),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(DaiflixSpacing.md)
    ) {
        Text(
            text = category.displayName,
            style = MaterialTheme.typography.headlineLarge,
            color = DaiflixColors.White
        )
        Text(
            text = "共 $totalCount 部 · 已筛选 $filteredCount 部",
            style = MaterialTheme.typography.labelLarge,
            color = DaiflixColors.TextTertiary,
            modifier = Modifier.padding(bottom = DaiflixSpacing.xxs)
        )
    }
}

@Composable
private fun CategoryGrid(
    items: List<VodItem>,
    gridState: LazyGridState,
    firstItemFocusRequester: FocusRequester,
    onNavigateUpToFilterBar: () -> Boolean,
    onVodClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val gap = DaiflixSpacing.md
        // Fit exactly six posters regardless of the panel's logical width.
        val cellWidth = (maxWidth - DaiflixSpacing.screenPadding * 2 - gap * (GRID_COLUMNS - 1)) / GRID_COLUMNS
        val cellHeight = cellWidth * 1.5f

        LazyVerticalGrid(
            columns = GridCells.Fixed(GRID_COLUMNS),
            state = gridState,
            contentPadding = PaddingValues(
                start = DaiflixSpacing.screenPadding,
                end = DaiflixSpacing.screenPadding,
                top = DaiflixSpacing.xs,
                bottom = DaiflixSpacing.xxxl
            ),
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalArrangement = Arrangement.spacedBy(DaiflixSpacing.lg),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                VodCard(
                    item = item,
                    onClick = { onVodClick(item.id) },
                    modifier = Modifier
                        .then(
                            if (index == 0) {
                                Modifier.focusRequester(firstItemFocusRequester)
                            } else {
                                Modifier
                            }
                        )
                        .onPreviewKeyEvent { event ->
                            // 首行按「上」显式移交到筛选条；其余情况（同列上一行）
                            // 返回 false 交给默认焦点搜索。
                            if (event.type == KeyEventType.KeyDown &&
                                event.key == Key.DirectionUp &&
                                index < GRID_COLUMNS
                            ) {
                                onNavigateUpToFilterBar()
                            } else {
                                false
                            }
                        },
                    width = cellWidth,
                    height = cellHeight
                )
            }
        }
    }
}

/** 显式移交焦点；目标未挂载/不可聚焦时返回 false，交回默认焦点搜索。 */
private fun requestFocusOrFalse(focusRequester: FocusRequester): Boolean =
    runCatching { focusRequester.requestFocus() }.isSuccess

@Composable
private fun CategoryError(
    message: String?,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    retryFocusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    // 错误页没有其它内容，首帧就把焦点交给「重试」，避免方向键无响应。
    RequestFocusWhenReady(retryFocusRequester)
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "加载失败",
                style = MaterialTheme.typography.headlineMedium,
                color = DaiflixColors.White
            )
            if (!message.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(DaiflixSpacing.sm))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = DaiflixColors.LightGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = DaiflixSpacing.xxxl)
                )
            }
            Spacer(modifier = Modifier.height(DaiflixSpacing.lg))
            Row(horizontalArrangement = Arrangement.spacedBy(DaiflixSpacing.md)) {
                DaiflixPrimaryButton(
                    text = "重试",
                    onClick = onRetry,
                    modifier = Modifier.focusRequester(retryFocusRequester)
                )
                DaiflixSecondaryButton(text = "返回首页", onClick = onBack)
            }
        }
    }
}
