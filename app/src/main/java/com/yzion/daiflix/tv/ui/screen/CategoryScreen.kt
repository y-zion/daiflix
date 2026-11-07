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
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.yzion.daiflix.tv.data.model.Category
import com.yzion.daiflix.tv.data.model.VodItem
import com.yzion.daiflix.tv.ui.component.DaiflixPrimaryButton
import com.yzion.daiflix.tv.ui.component.DaiflixSecondaryButton
import com.yzion.daiflix.tv.ui.component.EmptyState
import com.yzion.daiflix.tv.ui.component.LoadingState
import com.yzion.daiflix.tv.ui.component.RequestFocusWhenReady
import com.yzion.daiflix.tv.ui.component.TopNavBar
import com.yzion.daiflix.tv.ui.component.VodCard
import com.yzion.daiflix.tv.ui.theme.DaiflixColors
import com.yzion.daiflix.tv.ui.theme.DaiflixSpacing
import com.yzion.daiflix.tv.ui.viewmodel.CategoryViewModel

/** Six posters per row: two full rows + a peek of the third fit a 16:9 TV. */
private const val GRID_COLUMNS = 6

/**
 * Category browse: a dense, focus-scrollable poster grid under the solid
 * navigation bar, with a title/count header and proper loading/error/empty states.
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
    // 空态 / 错误态的「重试 / 返回首页」是页面上仅有的可聚焦控件，必须由它接管焦点。
    val emptyActionFocus = remember { FocusRequester() }

    // 网格首次出现（加载完成）后立刻聚焦第一张卡片：等一帧再请求，
    // 卡片自身的聚焦订阅已建立，焦点样式首帧即可见（审计 P1-3）。
    RequestFocusWhenReady(
        focusRequester = firstCardFocusRequester,
        key = uiState.items.isEmpty(),
        enabled = uiState.items.isNotEmpty()
    )

    LaunchedEffect(category) {
        viewModel.loadCategory(category)
        // A new category starts at the top instead of inheriting the old offset.
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
                    CategoryGrid(
                        category = category,
                        items = uiState.items,
                        gridState = gridState,
                        firstItemFocusRequester = firstCardFocusRequester,
                        onVodClick = onVodClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryGrid(
    category: Category,
    items: List<VodItem>,
    gridState: LazyGridState,
    firstItemFocusRequester: FocusRequester,
    onVodClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = DaiflixSpacing.screenPadding,
                    end = DaiflixSpacing.screenPadding,
                    top = DaiflixSpacing.lg,
                    bottom = DaiflixSpacing.md
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
                text = "共 ${items.size} 部",
                style = MaterialTheme.typography.labelLarge,
                color = DaiflixColors.TextTertiary,
                modifier = Modifier.padding(bottom = DaiflixSpacing.xxs)
            )
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
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
                        modifier = if (index == 0) {
                            Modifier.focusRequester(firstItemFocusRequester)
                        } else {
                            Modifier
                        },
                        width = cellWidth,
                        height = cellHeight
                    )
                }
            }
        }
    }
}

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
