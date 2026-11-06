package com.yzion.daiflix.tv.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.yzion.daiflix.tv.data.model.VodItem
import com.yzion.daiflix.tv.ui.theme.BannerDimensions
import com.yzion.daiflix.tv.ui.theme.DaiflixColors
import com.yzion.daiflix.tv.ui.theme.DaiflixSpacing
import kotlinx.coroutines.delay

private const val HERO_ROTATE_MS = 8_000L
private const val HERO_FADE_MS = 420

/**
 * Full-bleed hero used by Home.
 *
 * It is deliberately *not* part of the scrolling list: the rows slide over it, which
 * keeps the D-pad from scrolling the artwork out from under the transparent nav when
 * focus moves from the nav bar to the Play button.
 */
@Composable
fun HeroSection(
    banners: List<VodItem>,
    onVodClick: (String) -> Unit,
    onPlayClick: (String) -> Unit,
    height: Dp,
    modifier: Modifier = Modifier,
    playFocusRequester: FocusRequester? = null,
    onNavigateUp: (() -> Unit)? = null,
    onNavigateDown: (() -> Unit)? = null,
    onPlayFocusChange: ((Boolean) -> Unit)? = null,
    visibilityAlpha: Float = 1f
) {
    if (banners.isEmpty()) return

    var index by remember(banners) { mutableStateOf(0) }
    var contentVisible by remember { mutableStateOf(true) }
    val contentAlpha by animateFloatAsState(
        targetValue = if (contentVisible) 1f else 0f,
        animationSpec = tween(durationMillis = HERO_FADE_MS),
        label = "hero-content-alpha"
    )

    LaunchedEffect(banners.size) {
        if (banners.size <= 1) return@LaunchedEffect
        while (true) {
            delay(HERO_ROTATE_MS)
            contentVisible = false
            delay(HERO_FADE_MS.toLong() + 60L)
            index = (index + 1) % banners.size
            contentVisible = true
        }
    }

    val current = banners[index.coerceIn(0, banners.lastIndex)]

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .alpha(visibilityAlpha)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(contentAlpha)
        ) {
            AsyncImage(
                model = current.coverUrl,
                contentDescription = current.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        HorizontalScrim(modifier = Modifier.fillMaxSize())
        BottomScrim(modifier = Modifier.fillMaxSize(), strength = 0.9f)

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(BannerDimensions.heroGradientHeight)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.55f to DaiflixColors.Background.copy(alpha = 0.75f),
                        1f to DaiflixColors.Background
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(
                    start = DaiflixSpacing.screenPadding,
                    end = DaiflixSpacing.screenPadding,
                    bottom = DaiflixSpacing.xl
                )
        ) {
            Box(modifier = Modifier.alpha(contentAlpha)) {
                Column {
                    Text(
                        text = "DAIFLIX",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 3.sp
                        ),
                        color = DaiflixColors.Red
                    )
                    Spacer(modifier = Modifier.height(DaiflixSpacing.sm))
                    Text(
                        text = current.title,
                        style = if (current.title.length > 10) {
                            MaterialTheme.typography.displaySmall
                        } else {
                            MaterialTheme.typography.displayMedium
                        },
                        color = DaiflixColors.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(DaiflixSpacing.sm))
                    HeroInfoRow(item = current)
                    if (current.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(DaiflixSpacing.xs))
                        Text(
                            text = current.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = DaiflixColors.LightGray,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 660.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(DaiflixSpacing.lg))

            Row(
                horizontalArrangement = Arrangement.spacedBy(DaiflixSpacing.md),
                modifier = Modifier.onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionUp -> if (onNavigateUp != null) {
                            onNavigateUp()
                            true
                        } else {
                            false
                        }

                        Key.DirectionDown -> if (onNavigateDown != null) {
                            onNavigateDown()
                            true
                        } else {
                            false
                        }

                        else -> false
                    }
                }
            ) {
                HeroActionRing {
                    DaiflixPrimaryButton(
                        text = "播放",
                        onClick = { onPlayClick(current.id) },
                        icon = Icons.Rounded.PlayArrow,
                        modifier = (if (playFocusRequester != null) {
                            Modifier.focusRequester(playFocusRequester)
                        } else {
                            Modifier
                        }).onFocusChanged { onPlayFocusChange?.invoke(it.hasFocus) }
                            .onPreviewKeyEvent { event ->
                            if (onNavigateUp != null &&
                                event.type == KeyEventType.KeyDown &&
                                event.key == Key.DirectionUp
                            ) {
                                onNavigateUp()
                                true
                            } else {
                                false
                            }
                        }
                    )
                }
                HeroActionRing {
                    DaiflixSecondaryButton(
                        text = "详情",
                        onClick = { onVodClick(current.id) },
                        icon = Icons.Rounded.Info
                    )
                }
            }
        }

        if (banners.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = DaiflixSpacing.screenPadding, bottom = DaiflixSpacing.xl),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DaiflixSpacing.xs)
            ) {
                repeat(banners.size) { dotIndex ->
                    val selected = dotIndex == index.coerceIn(0, banners.lastIndex)
                    Box(
                        modifier = Modifier
                            .height(DaiflixSpacing.xxs)
                            .width(if (selected) DaiflixSpacing.lg else DaiflixSpacing.xs)
                            .clip(RoundedCornerShape(DaiflixSpacing.xxs))
                            .background(
                                if (selected) {
                                    DaiflixColors.White
                                } else {
                                    DaiflixColors.White.copy(alpha = 0.35f)
                                }
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroInfoRow(item: VodItem) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DaiflixSpacing.xs)
    ) {
        if (item.rating.isNotBlank()) {
            MetaPill(text = item.rating, color = DaiflixColors.Gold)
        }
        if (item.year.isNotBlank()) {
            Text(
                text = item.year,
                style = MaterialTheme.typography.labelLarge,
                color = DaiflixColors.LightGray
            )
        }
        if (item.episodeCount.isMeaningfulHeroLabel()) {
            HeroSeparatorDot()
            Text(
                text = item.episodeCount,
                style = MaterialTheme.typography.labelLarge,
                color = DaiflixColors.LightGray
            )
        }
        if (item.updateStatus.isNotBlank()) {
            HeroSeparatorDot()
            Text(
                text = item.updateStatus,
                style = MaterialTheme.typography.labelLarge,
                color = DaiflixColors.LightGray
            )
        }
    }
}

@Composable
private fun HeroSeparatorDot() {
    Box(
        modifier = Modifier
            .width(DaiflixSpacing.xxs)
            .height(DaiflixSpacing.xxs)
            .clip(RoundedCornerShape(50))
            .background(DaiflixColors.MediumGray)
    )
}

@Composable
private fun HeroActionRing(content: @Composable () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .onFocusChanged { focused = it.hasFocus }
            .then(
                if (focused) {
                    Modifier.border(
                        border = BorderStroke(2.dp, DaiflixColors.White),
                        shape = RoundedCornerShape(8.dp)
                    )
                } else {
                    Modifier
                }
            )
            .padding(DaiflixSpacing.xs)
    ) {
        content()
    }
}

/** Only badge a hero label when it really describes episodes, not a region caption. */
private fun String.isMeaningfulHeroLabel(): Boolean {
    val text = trim()
    if (text.isEmpty() || text.length > 10) return false
    if (HERO_LABEL_HINTS.any { text.contains(it) }) return true
    return text.all { it.isDigit() }
}

private val HERO_LABEL_HINTS = listOf(
    "集", "期", "话", "更新", "全", "正片", "预告", "HD", "BD", "TC", "TS"
)
