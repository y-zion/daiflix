package com.yzion.daiflix.tv.ui.component.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.yzion.daiflix.tv.data.model.PlaySource
import com.yzion.daiflix.tv.ui.theme.DaiflixColors
import com.yzion.daiflix.tv.ui.theme.DaiflixSpacing
import kotlinx.coroutines.delay

/** Bottom-sheet source switcher; the active source is marked in red. */
@Composable
fun SourcePanel(
    sources: List<PlaySource>,
    currentIndex: Int,
    onSelect: (Int) -> Unit,
    onClose: () -> Unit
) {
    val listState = rememberLazyListState()
    val focusRequesters = remember(sources.size) {
        List(sources.size.coerceAtLeast(1)) { FocusRequester() }
    }

    LaunchedEffect(sources.size, currentIndex) {
        if (sources.isEmpty()) return@LaunchedEffect
        val target = currentIndex.coerceIn(0, sources.lastIndex)
        // The sheet slides in; wait for the animation before requesting row focus.
        delay(280)
        runCatching { listState.scrollToItem(target) }
        delay(120)
        runCatching { focusRequesters[target].requestFocus() }
        delay(300)
        if (currentIndex == target) {
            runCatching { focusRequesters[target].requestFocus() }
        }
    }

    PlayerSheet(title = "换源", onClose = onClose) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 360.dp),
            verticalArrangement = Arrangement.spacedBy(DaiflixSpacing.xs)
        ) {
            itemsIndexed(sources) { index, source ->
                SourceRow(
                    name = source.name.ifBlank { "线路 ${index + 1}" },
                    episodeCount = source.episodes.size,
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
private fun SourceRow(
    name: String,
    episodeCount: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) DaiflixColors.DarkCard else DaiflixColors.DarkSurface,
            contentColor = DaiflixColors.White,
            focusedContainerColor = DaiflixColors.White,
            focusedContentColor = DaiflixColors.Black
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f)
    ) {
        // fillMaxSize() for the same reason as EpisodePanel's chip (audit P1-8): the
        // Box must cover the whole Surface, otherwise the Surface puts the shrink-wrapped
        // child at TopStart. The Row already fills the width/height, this makes it explicit.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = DaiflixSpacing.md)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (selected) "当前 · 共$episodeCount 集" else "共$episodeCount 集",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) DaiflixColors.Red else DaiflixColors.LightGray
                )
            }
        }
    }
}
