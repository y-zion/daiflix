package com.yzion.daiflix.tv.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.yzion.daiflix.tv.ui.theme.DaiflixColors
import com.yzion.daiflix.tv.ui.theme.DaiflixSpacing
import com.yzion.daiflix.tv.ui.viewmodel.CategorySort
import com.yzion.daiflix.tv.ui.viewmodel.YearFilterOption

private val ChipCornerRadius = 18.dp
private val ChipHorizontalPadding = 12.dp
private val ChipVerticalPadding = 8.dp
private val SeparatorHeight = 18.dp

/**
 * 分类页的客户端筛选条：年份档位 + 排序方式，全部作用于已加载的本地数据，
 * 不会重新请求网络、也不会进入 loading（审计 P2-8）。
 *
 * 焦点约定：
 * - chip 的焦点样式统一走 [rememberFocusState] + [Modifier.trackFocus] +
 *   [Modifier.focusHalo]（不用 `collectIsFocusedAsState`，见 FocusSupport.kt）。
 * - DPAD 下：由本组件用 `onPreviewKeyEvent` 显式把焦点交给网格第一张卡片；
 *   移交失败（例如筛选为空、网格未挂载）时返回 false，交回默认焦点搜索。
 * - DPAD 上/左右：不拦截，交给默认焦点搜索（上方 / 相邻 chip）。
 *
 * @param firstChipFocusRequester 网格首行按「上」时显式聚焦的目标，挂在第一个 chip 上。
 * @param onNavigateDown 网格第一张卡片的显式聚焦；成功返回 true 并消费按键。
 */
@Composable
fun CategoryFilterBar(
    yearOptions: List<YearFilterOption>,
    selectedYearId: String,
    onYearSelected: (String) -> Unit,
    sort: CategorySort,
    onSortSelected: (CategorySort) -> Unit,
    firstChipFocusRequester: FocusRequester,
    onNavigateDown: () -> Boolean,
    modifier: Modifier = Modifier
) {
    // 数据里一个年份都没有时，年份筛选没有意义，只保留排序组。
    val showYearGroup = yearOptions.size > 1

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = DaiflixSpacing.screenPadding,
                end = DaiflixSpacing.screenPadding,
                top = DaiflixSpacing.xxs,
                bottom = DaiflixSpacing.xs
            )
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || event.key != Key.DirectionDown) {
                    return@onPreviewKeyEvent false
                }
                // 显式交给网格第一行；没有可聚焦的卡片时返回 false 让默认焦点搜索兜底。
                onNavigateDown()
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DaiflixSpacing.xs)
    ) {
        if (showYearGroup) {
            FilterGroupLabel(text = "年份")
            yearOptions.forEachIndexed { index, option ->
                FilterChip(
                    label = option.label,
                    selected = option.id == selectedYearId,
                    onClick = { onYearSelected(option.id) },
                    modifier = if (index == 0) {
                        Modifier.focusRequester(firstChipFocusRequester)
                    } else {
                        Modifier
                    }
                )
            }
            FilterSeparator()
            FilterGroupLabel(text = "排序")
        }
        CategorySort.entries.forEachIndexed { index, option ->
            FilterChip(
                label = option.label,
                selected = option == sort,
                onClick = { onSortSelected(option) },
                // 年份组被隐藏时，让排序组的第一项成为「上」的落点。
                modifier = if (!showYearGroup && index == 0) {
                    Modifier.focusRequester(firstChipFocusRequester)
                } else {
                    Modifier
                }
            )
        }
    }
}

@Composable
private fun FilterGroupLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = DaiflixColors.TextTertiary,
        modifier = Modifier.padding(start = DaiflixSpacing.xxs, end = DaiflixSpacing.xxs)
    )
}

@Composable
private fun FilterSeparator() {
    Box(
        modifier = Modifier
            .padding(horizontal = DaiflixSpacing.xxs)
            .width(1.dp)
            .height(SeparatorHeight)
            .background(DaiflixColors.Divider)
    )
}

/**
 * 单个筛选 chip：选中态品牌红底/白字，未选中 DarkCard/浅灰字；
 * 焦点用外沿白色 focusHalo（不用 tv-material 的 collectIsFocusedAsState）。
 */
@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focus = rememberFocusState()
    val containerColor = if (selected) DaiflixColors.Red else DaiflixColors.DarkCard
    val contentColor = if (selected) DaiflixColors.White else DaiflixColors.LightGray
    // 聚焦时略微提亮底色，配合白色 focusHalo，远距离也能看出焦点位置。
    val focusedContainerColor = if (selected) DaiflixColors.Red else DaiflixColors.SurfaceElevated

    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(ChipCornerRadius)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = containerColor,
            contentColor = contentColor,
            focusedContainerColor = focusedContainerColor,
            focusedContentColor = contentColor
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        // 注意顺序：trackFocus 必须位于 Surface 内部 clickable 之前的 modifier 链上。
        modifier = modifier
            .trackFocus(focus)
            .focusHalo(focus, cornerRadius = ChipCornerRadius)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
            maxLines = 1,
            modifier = Modifier.padding(
                horizontal = ChipHorizontalPadding,
                vertical = ChipVerticalPadding
            )
        )
    }
}
