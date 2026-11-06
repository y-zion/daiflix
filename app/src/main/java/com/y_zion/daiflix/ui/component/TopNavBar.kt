package com.y_zion.daiflix.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.y_zion.daiflix.data.model.Category
import com.y_zion.daiflix.ui.theme.DaiflixColors
import com.y_zion.daiflix.ui.theme.DaiflixSpacing

@Composable
fun TopNavBar(
    selectedCategory: Category = Category.HOME,
    onCategoryClick: (Category) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onHistoryClick: () -> Unit = {},
    onFavoritesClick: () -> Unit = {},
    modifier: Modifier = Modifier
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
            .height(64.dp)
            .background(DaiflixColors.Black.copy(alpha = 0.95f))
            .padding(horizontal = DaiflixSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "DAIFLIX",
            style = MaterialTheme.typography.headlineMedium,
            color = DaiflixColors.Red,
            modifier = Modifier.padding(end = DaiflixSpacing.xl)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(DaiflixSpacing.xs),
            modifier = Modifier.weight(1f)
        ) {
            items(navItems) { pair ->
                NavItem(
                    label = pair.second,
                    isSelected = selectedCategory == pair.first,
                    onClick = { onCategoryClick(pair.first) }
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(DaiflixSpacing.xs)) {
            NavIconButton(label = "历史", onClick = onHistoryClick)
            NavIconButton(label = "收藏", onClick = onFavoritesClick)
            NavIconButton(label = "搜索", onClick = onSearchClick)
            NavIconButton(label = "设置", onClick = onSettingsClick)
        }
    }
}

@Composable
private fun NavItem(label: String, isSelected: Boolean, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(20.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) DaiflixColors.White else Color.Transparent,
            focusedContainerColor = DaiflixColors.DarkCard
        ),
        modifier = Modifier.onFocusChanged { isFocused = it.isFocused || it.hasFocus }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = when {
                isSelected -> DaiflixColors.Black
                isFocused -> DaiflixColors.White
                else -> DaiflixColors.LightGray
            },
            modifier = Modifier.padding(horizontal = DaiflixSpacing.md, vertical = DaiflixSpacing.xs)
        )
    }
}

@Composable
private fun NavIconButton(label: String, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = DaiflixColors.DarkCard
        ),
        modifier = Modifier.onFocusChanged { isFocused = it.isFocused || it.hasFocus }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = if (isFocused) DaiflixColors.White else DaiflixColors.LightGray,
            modifier = Modifier.padding(horizontal = DaiflixSpacing.sm, vertical = DaiflixSpacing.xs)
        )
    }
}
