package com.yzion.daiflix.tv.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
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
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.yzion.daiflix.tv.data.model.VodItem
import com.yzion.daiflix.tv.ui.theme.BannerDimensions
import com.yzion.daiflix.tv.ui.theme.CardDimensions
import com.yzion.daiflix.tv.ui.theme.DaiflixColors
import com.yzion.daiflix.tv.ui.theme.DaiflixSpacing
import kotlinx.coroutines.delay

/** How long each hero banner stays on screen before cross-fading to the next. */
private const val HERO_ROTATE_INTERVAL_MS = 8_000L

/** Duration of the hero copy/artwork fade. */
private const val HERO_FADE_MS = 380

/**
 * Netflix-style portrait key-art card.
 *
 * The poster is 2:3, the whole card grows on TV focus (1.08x) and gets a white
 * inset focus ring from tv-material. A rating pill sits top-right, the episode
 * count bottom-right and an optional "continue watching" progress bar runs along
 * the bottom edge. The title lives *below* the poster and is part of the same
 * scaled surface, so parent rows must reserve vertical room for the scale.
 */
@Composable
fun VodCard(
    item: VodItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showTitle: Boolean = true,
    height: Dp = CardDimensions.posterHeight,
    width: Dp = CardDimensions.posterWidth,
    progress: Float? = null
) {
    val interaction = remember { MutableInteractionSource() }
    // Explicit focus state (see FocusSupport.kt): the tv-material FocusInteraction
    // is a replay-less SharedFlow, so a focus request issued from a parent
    // LaunchedEffect loses its first event and the first frame renders unfocused.
    val focus = rememberFocusState()
    val isFocused = focus.isFocused

    Surface(
        onClick = onClick,
        interactionSource = interaction,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(CardDimensions.cardCornerRadius)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = DaiflixColors.White,
            focusedContainerColor = Color.Transparent,
            focusedContentColor = DaiflixColors.White
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = CardDimensions.focusedScale),
        border = ClickableSurfaceDefaults.border(
            border = if (focus.isFocused) focusBorder() else Border.None,
            focusedBorder = focusBorder()
        ),
        modifier = modifier.trackFocus(focus)
    ) {
        Column(modifier = Modifier.width(width)) {
            Box(
                modifier = Modifier
                    .width(width)
                    .height(height)
                    .clip(RoundedCornerShape(CardDimensions.cardCornerRadius))
                    .background(DaiflixColors.DarkCard)
            ) {
                AsyncImage(
                    model = item.coverUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    placeholder = ColorPainter(DaiflixColors.DarkSurface),
                    error = ColorPainter(DaiflixColors.DarkCard),
                    modifier = Modifier.fillMaxSize()
                )

                // Keeps pills readable on bright artwork.
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(DaiflixSpacing.xxl)
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                1f to Color.Black.copy(alpha = 0.6f)
                            )
                        )
                )

                if (item.rating.isNotBlank()) {
                    PosterBadge(
                        text = item.rating,
                        color = DaiflixColors.Gold,
                        background = Color.Black.copy(alpha = 0.72f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(DaiflixSpacing.xs)
                    )
                }

                if (item.episodeCount.isMeaningfulEpisodeLabel()) {
                    PosterBadge(
                        text = item.episodeCount,
                        color = DaiflixColors.White,
                        background = DaiflixColors.Red.copy(alpha = 0.92f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(DaiflixSpacing.xs)
                    )
                }

                if (progress != null) {
                    ContinueWatchingBar(
                        progress = progress,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                    )
                }
            }

            if (showTitle) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isFocused) DaiflixColors.White else DaiflixColors.LightGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(
                        start = DaiflixSpacing.xxs,
                        end = DaiflixSpacing.xxs,
                        top = DaiflixSpacing.xs
                    )
                )
            }
        }
    }
}

/**
 * Horizontal carousel used by every browse screen.
 *
 * `progressById` lets Home show a resume bar on the "继续观看" row without
 * changing the [VodItem] model; other screens just omit it.
 */
@Composable
fun VodRow(
    title: String,
    items: List<VodItem>,
    onVodClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    progressById: Map<String, Float> = emptyMap(),
    firstItemFocusRequester: FocusRequester? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = title)
        LazyRow(
            contentPadding = PaddingValues(
                horizontal = DaiflixSpacing.screenPadding,
                // Room for the 1.08x focus scale so cards never clip vertically.
                vertical = DaiflixSpacing.md
            ),
            horizontalArrangement = Arrangement.spacedBy(DaiflixSpacing.md),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(items, key = { _, vod -> vod.id }) { index, vod ->
                VodCard(
                    item = vod,
                    onClick = { onVodClick(vod.id) },
                    progress = progressById[vod.id],
                    modifier = if (index == 0 && firstItemFocusRequester != null) {
                        Modifier.focusRequester(firstItemFocusRequester)
                    } else {
                        Modifier
                    }
                )
            }
        }
    }
}

/**
 * Full-bleed hero for the Home screen.
 *
 * Up to [banners] rotate every [HERO_ROTATE_INTERVAL_MS] with a soft fade; the
 * copy sits bottom-left over layered scrims that melt into the page background,
 * and D-pad actions live underneath. When there is a single banner the dots and
 * the rotation are hidden.
 */
@Composable
fun HeroBanner(
    banners: List<VodItem>,
    onVodClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = BannerDimensions.heroHeight,
    onPlayClick: (String) -> Unit = onVodClick,
    playFocusRequester: FocusRequester? = null,
    onNavigateUp: (() -> Unit)? = null
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
            delay(HERO_ROTATE_INTERVAL_MS)
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
    ) {
        // Artwork fades independently of the buttons so focus never dims.
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

        // Final fade into the page colour so the hero does not end on a hard edge.
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
                .fillMaxWidth(0.58f)
                .padding(
                    start = DaiflixSpacing.screenPadding,
                    end = DaiflixSpacing.xl,
                    bottom = DaiflixSpacing.xxl
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
                    HeroMetaRow(item = current)
                    if (current.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(DaiflixSpacing.xs))
                        Text(
                            text = current.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = DaiflixColors.LightGray,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(DaiflixSpacing.lg))

            Row(
                horizontalArrangement = Arrangement.spacedBy(DaiflixSpacing.md),
                modifier = Modifier.onPreviewKeyEvent { event ->
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
            ) {
                HeroFocusRing {
                    DaiflixPrimaryButton(
                        text = "播放",
                        onClick = { onPlayClick(current.id) },
                        icon = Icons.Rounded.PlayArrow,
                        modifier = (if (playFocusRequester != null) {
                            Modifier.focusRequester(playFocusRequester)
                        } else {
                            Modifier
                        })
                    )
                }
                HeroFocusRing {
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

/** Rating / year / episode / status line used by the hero. */
@Composable
private fun HeroMetaRow(item: VodItem) {
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
        if (item.episodeCount.isMeaningfulEpisodeLabel()) {
            HeroDot()
            Text(
                text = item.episodeCount,
                style = MaterialTheme.typography.labelLarge,
                color = DaiflixColors.LightGray
            )
        }
        if (item.updateStatus.isNotBlank()) {
            HeroDot()
            Text(
                text = item.updateStatus,
                style = MaterialTheme.typography.labelLarge,
                color = DaiflixColors.LightGray
            )
        }
    }
}

@Composable
private fun HeroDot() {
    Text(
        text = "·",
        style = MaterialTheme.typography.labelLarge,
        color = DaiflixColors.MediumGray
    )
}

/**
 * Draws a white focus ring a few dp *outside* the wrapped action so it stays
 * visible even around the solid-white primary "播放" button.
 */
@Composable
private fun HeroFocusRing(content: @Composable () -> Unit) {
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
            .padding(DaiflixSpacing.xxs)
    ) {
        content()
    }
}

/** Gold rating / red episode pill rendered over the poster. */
@Composable
private fun PosterBadge(
    text: String,
    color: Color,
    background: Color,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        color = color,
        maxLines = 1,
        modifier = modifier
            .background(background, RoundedCornerShape(DaiflixSpacing.xxs))
            .padding(horizontal = DaiflixSpacing.xs, vertical = DaiflixSpacing.xxxs)
    )
}

/** Thin resume bar pinned to the bottom edge of a "继续观看" poster. */
@Composable
private fun ContinueWatchingBar(progress: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(DaiflixSpacing.xxs)
            .background(Color.Black.copy(alpha = 0.65f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0.02f, 1f))
                .fillMaxHeight()
                .background(DaiflixColors.Red)
        )
    }
}

/**
 * Some list templates put the region caption in the slot an episode label normally
 * occupies ("中国大陆", "波兰"), so only badge text that actually describes episodes.
 */
private fun String.isMeaningfulEpisodeLabel(): Boolean {
    val text = trim()
    if (text.isEmpty() || text.length > 10) return false
    if (EPISODE_LABEL_HINTS.any { text.contains(it) }) return true
    return text.all { it.isDigit() }
}

private val EPISODE_LABEL_HINTS = listOf(
    "集", "期", "话", "更新", "全", "正片", "预告", "HD", "BD", "TC", "TS"
)
