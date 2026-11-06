package com.y_zion.daiflix.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.y_zion.daiflix.data.model.VodItem
import com.y_zion.daiflix.ui.theme.CardDimensions
import com.y_zion.daiflix.ui.theme.DaiflixColors
import com.y_zion.daiflix.ui.theme.DaiflixSpacing

@Composable
fun VodCard(
    item: VodItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showTitle: Boolean = true,
    height: Dp = CardDimensions.posterHeight,
    width: Dp = CardDimensions.posterWidth
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(CardDimensions.cardCornerRadius)),
        modifier = modifier
            .width(width)
            .onFocusChanged { isFocused = it.isFocused || it.hasFocus }
    ) {
        Column {
            Box(
                modifier = Modifier
                    .width(width)
                    .height(height * 0.75f)
                    .clip(RoundedCornerShape(topStart = CardDimensions.cardCornerRadius, topEnd = CardDimensions.cardCornerRadius))
                    .background(DaiflixColors.DarkCard)
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
                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (item.episodeCount.isNotBlank()) {
                    Text(
                        text = item.episodeCount,
                        style = MaterialTheme.typography.labelSmall,
                        color = DaiflixColors.White,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(DaiflixSpacing.xs)
                            .background(DaiflixColors.Red.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (isFocused) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(3.dp, DaiflixColors.White, RoundedCornerShape(CardDimensions.cardCornerRadius))
                    )
                }
            }

            if (showTitle) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isFocused) DaiflixColors.White else DaiflixColors.LightGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(
                        start = DaiflixSpacing.xs,
                        end = DaiflixSpacing.xs,
                        top = DaiflixSpacing.xs,
                        bottom = DaiflixSpacing.xxs
                    )
                )
            }
        }
    }
}

@Composable
fun VodRow(
    title: String,
    items: List<VodItem>,
    onVodClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = DaiflixColors.White,
            modifier = Modifier.padding(
                start = DaiflixSpacing.lg,
                top = DaiflixSpacing.md,
                bottom = DaiflixSpacing.sm
            )
        )

        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = DaiflixSpacing.lg),
            horizontalArrangement = Arrangement.spacedBy(DaiflixSpacing.sm),
            modifier = Modifier.height(CardDimensions.posterHeight + 40.dp)
        ) {
            items(items, key = { it.id }) { vod ->
                VodCard(item = vod, onClick = { onVodClick(vod.id) })
            }
        }
    }
}

@Composable
fun HeroBanner(
    item: VodItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        modifier = modifier
            .fillMaxWidth()
            .height(400.dp)
            .onFocusChanged { isFocused = it.isFocused || it.hasFocus }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = item.coverUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.9f),
                                Color.Black.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                            startY = 200f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(DaiflixSpacing.xl)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.displaySmall,
                    color = DaiflixColors.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (item.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(DaiflixSpacing.xs))
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = DaiflixColors.LightGray,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(DaiflixSpacing.md))

                Surface(
                    onClick = onClick,
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = DaiflixColors.White,
                        focusedContainerColor = DaiflixColors.Red
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = DaiflixSpacing.lg, vertical = DaiflixSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(DaiflixSpacing.xs)
                    ) {
                        Text(
                            text = "▶  播放",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isFocused) DaiflixColors.White else DaiflixColors.Black
                        )
                    }
                }
            }

            if (isFocused) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(3.dp, DaiflixColors.White, RoundedCornerShape(12.dp))
                )
            }
        }
    }
}
