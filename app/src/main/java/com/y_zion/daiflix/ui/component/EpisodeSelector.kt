package com.y_zion.daiflix.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.y_zion.daiflix.data.model.Episode
import com.y_zion.daiflix.data.model.PlaySource
import com.y_zion.daiflix.ui.theme.DaiflixColors
import com.y_zion.daiflix.ui.theme.DaiflixSpacing

/**
 * 集数选择器
 */
@Composable
fun EpisodeSelector(
    sources: List<PlaySource>,
    selectedSourceIndex: Int,
    selectedEpisodeIndex: Int,
    onSourceChange: (Int) -> Unit,
    onEpisodeClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        if (sources.isEmpty()) return

        // 播放源切换
        if (sources.size > 1) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(DaiflixSpacing.xs),
                modifier = Modifier.padding(bottom = DaiflixSpacing.sm)
            ) {
                sources.forEachIndexed { index, source ->
                    SourceTab(
                        name = source.name,
                        isSelected = index == selectedSourceIndex,
                        onClick = { onSourceChange(index) }
                    )
                }
            }
        }

        // 集数列表
        val currentSource = sources.getOrNull(selectedSourceIndex) ?: return
        val episodes = currentSource.episodes

        // 使用网格布局展示集数
        val columns = 8
        val rows = (episodes.size + columns - 1) / columns

        for (row in 0 until rows) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(DaiflixSpacing.xs),
                modifier = Modifier.padding(vertical = DaiflixSpacing.xxs)
            ) {
                for (col in 0 until columns) {
                    val epIndex = row * columns + col
                    if (epIndex < episodes.size) {
                        EpisodeChip(
                            episode = episodes[epIndex],
                            isSelected = epIndex == selectedEpisodeIndex,
                            onClick = { onEpisodeClick(epIndex) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceTab(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) DaiflixColors.Red else DaiflixColors.DarkCard,
            focusedContainerColor = if (isSelected) DaiflixColors.DarkRed else DaiflixColors.DarkSurface
        ),
        modifier = Modifier.onFocusChanged { isFocused = it.isFocused || it.hasFocus }
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelLarge,
            color = DaiflixColors.White,
            modifier = Modifier.padding(horizontal = DaiflixSpacing.md, vertical = DaiflixSpacing.xs)
        )
    }
}

@Composable
private fun EpisodeChip(
    episode: Episode,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = when {
                isSelected -> DaiflixColors.Red
                isFocused -> DaiflixColors.DarkSurface
                else -> DaiflixColors.DarkCard
            },
            focusedContainerColor = DaiflixColors.DarkSurface
        ),
        modifier = modifier.onFocusChanged { isFocused = it.isFocused || it.hasFocus }
    ) {
        Box(
            modifier = Modifier.padding(vertical = DaiflixSpacing.xs),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Text(
                text = episode.name,
                style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) DaiflixColors.White else DaiflixColors.LightGray
            )
        }
    }
}
