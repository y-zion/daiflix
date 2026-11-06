package com.yzion.daiflix.tv.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.yzion.daiflix.tv.data.model.Category
import com.yzion.daiflix.tv.ui.theme.DaiflixColors
import com.yzion.daiflix.tv.ui.theme.DaiflixSpacing
import com.yzion.daiflix.tv.ui.theme.NavigationDimensions

/**
 * Netflix-style top navigation.
 *
 * - Brand lockup on the left, text-only browse tabs in the middle, utilities right.
 * - Transparent over the hero on Home; solid on plain backgrounds.
 * - Selected tab is white + bold; focused tab scales up and turns white.
 */
@Composable
fun TopNavBar(
    selectedCategory: Category = Category.HOME,
    onCategoryClick: (Category) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onHistoryClick: () -> Unit = {},
    onFavoritesClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    transparent: Boolean = false,
    firstTabFocusRequester: FocusRequester? = null,
    onNavigateDown: (() -> Unit)? = null
) {
    val navItems = listOf(
        Category.HOME to "首页",
        Category.MOVIE to "电影",
        Category.TV_SERIES to "电视剧",
        Category.VARIETY to "综艺",
        Category.ANIME to "动漫"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(NavigationDimensions.topNavHeight)
            .background(
                if (transparent) {
                    Brush.verticalGradient(
                        0f to DaiflixColors.Black.copy(alpha = 0.85f),
                        0.7f to DaiflixColors.Black.copy(alpha = 0.25f),
                        1f to Color.Transparent
                    )
                } else {
                    Brush.verticalGradient(listOf(DaiflixColors.Background, DaiflixColors.Background))
                }
            )
            .padding(horizontal = DaiflixSpacing.screenPadding)
            // Focus search out of an overlay row is unreliable on some panels, so the
            // bar explicitly hands DOWN over to the hero actions.
            .onPreviewKeyEvent { event ->
                if (onNavigateDown != null &&
                    event.type == KeyEventType.KeyDown &&
                    event.key == Key.DirectionDown
                ) {
                    onNavigateDown()
                    true
                } else {
                    false
                }
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        BrandLogo(height = 30.dp)
        Spacer(modifier = Modifier.width(40.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            itemsIndexed(navItems) { index, pair ->
                NavItem(
                    label = pair.second,
                    isSelected = selectedCategory == pair.first,
                    onClick = { onCategoryClick(pair.first) },
                    modifier = if (index == 0 && firstTabFocusRequester != null) {
                        Modifier.focusRequester(firstTabFocusRequester)
                    } else {
                        Modifier
                    }
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem(label = "搜索", isSelected = false, onClick = onSearchClick, compact = true)
            NavItem(label = "历史", isSelected = false, onClick = onHistoryClick, compact = true)
            NavItem(label = "收藏", isSelected = false, onClick = onFavoritesClick, compact = true)
            NavItem(label = "设置", isSelected = false, onClick = onSettingsClick, compact = true)
        }
    }
}

@Composable
private fun NavItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val color = when {
        isSelected -> DaiflixColors.White
        focused -> DaiflixColors.White
        else -> DaiflixColors.LightGray
    }

    Surface(
        onClick = onClick,
        interactionSource = interaction,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(6.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = color,
            focusedContainerColor = Color.Transparent,
            focusedContentColor = DaiflixColors.White,
            pressedContainerColor = Color.Transparent,
            pressedContentColor = DaiflixColors.White
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.10f),
        modifier = modifier
    ) {
        Text(
            text = label,
            style = if (compact) {
                MaterialTheme.typography.titleSmall
            } else {
                MaterialTheme.typography.titleMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            },
            color = color,
            modifier = Modifier.padding(
                horizontal = if (compact) DaiflixSpacing.xs else DaiflixSpacing.sm,
                vertical = DaiflixSpacing.xs
            )
        )
    }
}
