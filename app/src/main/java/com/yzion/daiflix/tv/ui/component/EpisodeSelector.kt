package com.yzion.daiflix.tv.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.yzion.daiflix.tv.data.model.PlaySource
import com.yzion.daiflix.tv.ui.theme.DaiflixColors
import com.yzion.daiflix.tv.ui.theme.DaiflixSpacing

private const val EPISODES_PER_ROW = 8

/**
 * TV episode picker: optional source tabs on top of an 8-column grid of episode
 * chips. Every chip is D-pad focusable with a 2dp white ring, the currently
 * selected episode is filled red and the last watched episode keeps a red
 * outline even when another episode is selected.
 */
@Composable
fun EpisodeSelector(
    sources: List<PlaySource>,
    selectedSourceIndex: Int,
    selectedEpisodeIndex: Int,
    onSourceChange: (Int) -> Unit,
    onEpisodeClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    lastWatchedSourceIndex: Int = -1,
    lastWatchedEpisodeIndex: Int = -1
) {
    val episodes = sources.getOrNull(selectedSourceIndex)?.episodes.orEmpty()

    Column(modifier = modifier.fillMaxWidth()) {
        if (sources.size > 1) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(DaiflixSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                sources.forEachIndexed { index, source ->
                    SourceTab(
                        name = source.name.ifBlank { "线路 ${index + 1}" },
                        selected = index == selectedSourceIndex,
                        onClick = { onSourceChange(index) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(DaiflixSpacing.md))
        }

        if (episodes.isEmpty()) {
            Text(
                text = "暂无可播放剧集",
                style = MaterialTheme.typography.bodyMedium,
                color = DaiflixColors.MediumGray,
                modifier = Modifier.padding(vertical = DaiflixSpacing.sm)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(DaiflixSpacing.xs)) {
                episodes.chunked(EPISODES_PER_ROW).forEachIndexed { rowIndex, rowEpisodes ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(DaiflixSpacing.xs)
                    ) {
                        rowEpisodes.forEachIndexed { columnIndex, episode ->
                            val episodeIndex = rowIndex * EPISODES_PER_ROW + columnIndex
                            EpisodeChip(
                                label = episode.name.ifBlank { "第${episodeIndex + 1}集" },
                                selected = episodeIndex == selectedEpisodeIndex,
                                lastWatched = selectedSourceIndex == lastWatchedSourceIndex &&
                                    episodeIndex == lastWatchedEpisodeIndex,
                                onClick = { onEpisodeClick(episodeIndex) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Keep the last row aligned with the 8-column grid.
                        repeat(EPISODES_PER_ROW - rowEpisodes.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceTab(
    name: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(50)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) DaiflixColors.Red else DaiflixColors.DarkCard,
            contentColor = DaiflixColors.White,
            focusedContainerColor = DaiflixColors.White,
            focusedContentColor = DaiflixColors.Black,
            pressedContainerColor = DaiflixColors.DarkRed,
            pressedContentColor = DaiflixColors.White
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
        border = ClickableSurfaceDefaults.border(focusedBorder = focusBorder())
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(
                horizontal = DaiflixSpacing.md,
                vertical = DaiflixSpacing.xs
            )
        )
    }
}

@Composable
private fun EpisodeChip(
    label: String,
    selected: Boolean,
    lastWatched: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(6.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) DaiflixColors.Red else DaiflixColors.DarkCard,
            contentColor = DaiflixColors.White,
            focusedContainerColor = DaiflixColors.White,
            focusedContentColor = DaiflixColors.Black,
            pressedContainerColor = DaiflixColors.DarkRed,
            pressedContentColor = DaiflixColors.White
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
        border = ClickableSurfaceDefaults.border(
            border = if (lastWatched && !selected) {
                focusBorder(DaiflixColors.Red)
            } else {
                Border.None
            },
            focusedBorder = focusBorder()
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = DaiflixSpacing.xxs),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
