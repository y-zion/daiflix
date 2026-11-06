package com.yzion.daiflix.tv.ui.component.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.yzion.daiflix.tv.data.model.Episode
import com.yzion.daiflix.tv.ui.theme.DaiflixColors
import com.yzion.daiflix.tv.ui.theme.DaiflixSpacing
import kotlinx.coroutines.delay

/** Bottom-sheet episode grid (6 columns), current episode highlighted in red. */
@Composable
fun EpisodePanel(
    episodes: List<Episode>,
    currentIndex: Int,
    sourceName: String,
    onSelect: (Int) -> Unit,
    onClose: () -> Unit
) {
    val gridState = rememberLazyGridState()
    val focusRequesters = remember(episodes.size) {
        List(episodes.size.coerceAtLeast(1)) { FocusRequester() }
    }

    LaunchedEffect(episodes.size, currentIndex) {
        if (episodes.isEmpty()) return@LaunchedEffect
        val target = currentIndex.coerceIn(0, episodes.lastIndex)
        // The sheet slides in; wait for the animation before requesting item focus.
        delay(280)
        runCatching { gridState.scrollToItem(target) }
        delay(120)
        runCatching { focusRequesters[target].requestFocus() }
        delay(300)
        if (currentIndex == target) {
            runCatching { focusRequesters[target].requestFocus() }
        }
    }

    PlayerSheet(
        title = if (sourceName.isBlank()) "选集" else "选集 · $sourceName",
        onClose = onClose
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            state = gridState,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 360.dp),
            contentPadding = PaddingValues(bottom = DaiflixSpacing.xs),
            horizontalArrangement = Arrangement.spacedBy(DaiflixSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(DaiflixSpacing.sm)
        ) {
            itemsIndexed(episodes) { index, episode ->
                EpisodeGridItem(
                    label = episode.name.ifBlank { "第${index + 1}集" },
                    selected = index == currentIndex,
                    onClick = { onSelect(index) },
                    modifier = Modifier.focusRequester(
                        focusRequesters[index.coerceIn(0, focusRequesters.lastIndex)]
                    )
                )
            }
        }
    }
}

@Composable
private fun EpisodeGridItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) DaiflixColors.Red else DaiflixColors.DarkCard,
            contentColor = DaiflixColors.White,
            focusedContainerColor = DaiflixColors.White,
            focusedContentColor = DaiflixColors.Black
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
