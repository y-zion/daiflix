package com.yzion.daiflix.tv.ui.component.player

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.ClosedCaptionOff
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.yzion.daiflix.tv.ui.theme.DaiflixColors
import com.yzion.daiflix.tv.ui.theme.DaiflixSpacing

/** Selectable controls of the custom TV HUD, in D-pad LEFT/RIGHT order. */
enum class PlayerControl {
    REWIND, PLAY, FORWARD, NEXT, EPISODES, SOURCES, DANMAKU, SPEED
}

val PLAYER_SPEED_STEPS = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

/** Content alpha of a disabled HUD control (audit P3-2: must read clearly dimmer). */
private const val DISABLED_CONTROL_ALPHA = 0.28f

/** "1.0" / "0.75" / "1.25" style label used by the speed control. */
fun playerSpeedLabel(speed: Float): String = when (speed) {
    1.0f -> "1.0x"
    0.75f -> "0.75x"
    1.25f -> "1.25x"
    1.5f -> "1.5x"
    2.0f -> "2.0x"
    else -> "${speed}x"
}

/** Steps through PLAYER_SPEED_STEPS and wraps around. */
fun nextPlayerSpeed(current: Float): Float {
    val index = PLAYER_SPEED_STEPS.indexOfFirst { kotlin.math.abs(it - current) < 0.01f }
    if (index < 0) return 1.0f
    return PLAYER_SPEED_STEPS[(index + 1) % PLAYER_SPEED_STEPS.size]
}

// ---------------------------------------------------------------------------- top bar

@Composable
fun PlayerTopBar(
    title: String,
    episodeLabel: String,
    sourceLabel: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Black.copy(alpha = 0.88f), Color.Transparent)
                )
            )
            .padding(
                start = DaiflixSpacing.screenPadding,
                end = DaiflixSpacing.screenPadding,
                top = DaiflixSpacing.lg,
                bottom = DaiflixSpacing.xxl
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DaiflixSpacing.md)
        ) {
            // Not focusable: BACK is handled globally, D-pad focus stays on the root anchor.
            Surface(
                onClick = onBack,
                modifier = Modifier
                    .size(44.dp)
                    .focusProperties { canFocus = false },
                shape = ClickableSurfaceDefaults.shape(CircleShape),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = Color.Black.copy(alpha = 0.5f),
                    contentColor = DaiflixColors.White
                )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                        contentDescription = "返回",
                        tint = DaiflixColors.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title.ifBlank { "正在播放" },
                    style = MaterialTheme.typography.titleMedium,
                    color = DaiflixColors.White,
                    maxLines = 1
                )
                if (episodeLabel.isNotBlank() || sourceLabel.isNotBlank()) {
                    Spacer(Modifier.height(DaiflixSpacing.xxxs))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(DaiflixSpacing.sm)
                    ) {
                        if (episodeLabel.isNotBlank()) {
                            Text(
                                text = episodeLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = DaiflixColors.LightGray
                            )
                        }
                        if (sourceLabel.isNotBlank()) {
                            Text(
                                text = sourceLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = DaiflixColors.TextTertiary
                            )
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------ bottom panel

@Composable
fun PlayerBottomBar(
    positionMs: Long,
    durationMs: Long,
    bufferedMs: Long,
    isPlaying: Boolean,
    speed: Float,
    danmakuEnabled: Boolean,
    danmakuCount: Int,
    hasNext: Boolean,
    selected: PlayerControl,
    onControlClick: (PlayerControl) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.92f))
                )
            )
            .padding(
                start = DaiflixSpacing.screenPadding,
                end = DaiflixSpacing.screenPadding,
                top = DaiflixSpacing.xxl,
                bottom = DaiflixSpacing.xl
            )
    ) {
        PlayerSeekBar(
            positionMs = positionMs,
            durationMs = durationMs,
            bufferedMs = bufferedMs,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(DaiflixSpacing.xxs))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatPlayerTime(positionMs),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                color = DaiflixColors.LightGray
            )
            Text(
                text = formatPlayerTime(durationMs),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                color = DaiflixColors.LightGray
            )
        }
        Spacer(Modifier.height(DaiflixSpacing.sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.Bottom
        ) {
            PlayerControlButton(
                selected = selected == PlayerControl.REWIND,
                icon = Icons.Rounded.FastRewind,
                label = "快退10s",
                onClick = { onControlClick(PlayerControl.REWIND) }
            )
            PlayerControlButton(
                selected = selected == PlayerControl.PLAY,
                icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                label = if (isPlaying) "暂停" else "播放",
                primary = true,
                onClick = { onControlClick(PlayerControl.PLAY) }
            )
            PlayerControlButton(
                selected = selected == PlayerControl.FORWARD,
                icon = Icons.Rounded.FastForward,
                label = "快进10s",
                onClick = { onControlClick(PlayerControl.FORWARD) }
            )
            PlayerControlButton(
                selected = selected == PlayerControl.NEXT,
                icon = Icons.Rounded.SkipNext,
                label = "下一集",
                enabled = hasNext,
                onClick = { onControlClick(PlayerControl.NEXT) }
            )
            PlayerControlButton(
                selected = selected == PlayerControl.EPISODES,
                icon = Icons.Rounded.PlaylistPlay,
                label = "选集",
                onClick = { onControlClick(PlayerControl.EPISODES) }
            )
            PlayerControlButton(
                selected = selected == PlayerControl.SOURCES,
                icon = Icons.Rounded.SwapHoriz,
                label = "换源",
                onClick = { onControlClick(PlayerControl.SOURCES) }
            )
            PlayerControlButton(
                selected = selected == PlayerControl.DANMAKU,
                icon = if (danmakuEnabled) Icons.Rounded.ClosedCaption else Icons.Rounded.ClosedCaptionOff,
                // Audit P2-9 follow-up: "弹幕开" alone was indistinguishable from a broken
                // feature when the source simply has no danmaku, so show the loaded count.
                label = when {
                    !danmakuEnabled -> "弹幕关"
                    danmakuCount > 0 -> "弹幕 $danmakuCount"
                    else -> "暂无弹幕"
                },
                onClick = { onControlClick(PlayerControl.DANMAKU) }
            )
            PlayerControlButton(
                selected = selected == PlayerControl.SPEED,
                icon = Icons.Rounded.Speed,
                label = playerSpeedLabel(speed),
                onClick = { onControlClick(PlayerControl.SPEED) }
            )
        }
    }
}

@Composable
private fun PlayerControlButton(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    primary: Boolean = false,
    enabled: Boolean = true
) {
    // Explicit disabled branch (audit P3-2): tv-material's default disabled look was
    // almost the same as "unfocused but available". Disabled content is dimmed to ~28%
    // alpha, gets no selection ring/scale and cannot be focused; the available unfocused
    // state keeps the current opaque light-grey label.
    val scale by animateFloatAsState(
        targetValue = if (selected && enabled) 1.08f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "playerControlScale"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(scale)
            .alpha(if (enabled) 1f else DISABLED_CONTROL_ALPHA)
    ) {
        val buttonSize = if (primary) 66.dp else 52.dp
        Box(
            modifier = Modifier
                .size(buttonSize + 10.dp)
                .border(
                    width = 2.dp,
                    color = if (selected && enabled) DaiflixColors.White else Color.Transparent,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                onClick = onClick,
                modifier = Modifier
                    .size(buttonSize)
                    .focusProperties { canFocus = false },
                enabled = enabled,
                shape = ClickableSurfaceDefaults.shape(CircleShape),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = if (primary) DaiflixColors.White
                    else Color.Black.copy(alpha = 0.55f),
                    contentColor = if (primary) DaiflixColors.Black
                    else DaiflixColors.White,
                    focusedContainerColor = if (primary) DaiflixColors.White
                    else Color.Black.copy(alpha = 0.55f),
                    focusedContentColor = if (primary) DaiflixColors.Black
                    else DaiflixColors.White
                ),
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (primary) DaiflixColors.Black else DaiflixColors.White,
                        modifier = Modifier.size(if (primary) 34.dp else 26.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(DaiflixSpacing.xxs))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected && enabled) DaiflixColors.White else DaiflixColors.LightGray,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

// ---------------------------------------------------------------------------- center OSD

@Composable
fun PlayerPausedOverlay(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Rounded.Pause,
                contentDescription = "已暂停",
                tint = DaiflixColors.White.copy(alpha = 0.92f),
                modifier = Modifier.size(80.dp)
            )
            Spacer(Modifier.height(DaiflixSpacing.xs))
            Text(
                text = "已暂停",
                style = MaterialTheme.typography.titleLarge,
                color = DaiflixColors.White.copy(alpha = 0.92f)
            )
        }
    }
}

@Composable
fun PlayerBufferingIndicator(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        BufferingRing()
    }
}

/**
 * Loading / episode-switch state (audit P2-9): ring + "正在加载 · 剧名 第N集".
 *
 * Previously the switch showed either a silent 64dp ring (buffering) or a generic
 * "正在解析播放地址..." without the episode, so users could not tell what was loading.
 */
@Composable
fun PlayerLoadingOverlay(label: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DaiflixColors.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BufferingRing()
            Spacer(Modifier.height(DaiflixSpacing.md))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = DaiflixColors.LightGray,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun BufferingRing(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "buffering")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing)
        ),
        label = "bufferingRotation"
    )
    Canvas(modifier = modifier.size(64.dp)) {
        drawArc(
            color = DaiflixColors.White.copy(alpha = 0.9f),
            startAngle = rotation,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
            size = androidx.compose.ui.geometry.Size(
                size.width - 8.dp.toPx(),
                size.height - 8.dp.toPx()
            ),
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
fun PlayerSeekFeedback(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .background(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(horizontal = DaiflixSpacing.lg, vertical = DaiflixSpacing.sm)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = DaiflixColors.White
            )
        }
    }
}
