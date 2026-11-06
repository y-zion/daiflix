package com.yzion.daiflix.tv.ui.component.player

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.yzion.daiflix.tv.ui.theme.DaiflixColors

private val SeekTrackShape = RoundedCornerShape(4.dp)

/** Formats milliseconds as mm:ss (or h:mm:ss when the media is longer than an hour). */
fun formatPlayerTime(millis: Long): String {
    val safe = if (millis <= 0L) 0L else millis
    val totalSeconds = safe / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "$hours:${pad2(minutes)}:${pad2(seconds)}"
    } else {
        "${pad2(minutes)}:${pad2(seconds)}"
    }
}

private fun pad2(value: Long): String = if (value < 10L) "0$value" else value.toString()

private fun fractionOf(value: Long, total: Long): Float =
    if (total <= 0L) 0f else (value.toFloat() / total.toFloat()).coerceIn(0f, 1f)

/**
 * Netflix-style seek bar: played part is white, buffered part grey, track dark grey.
 * The bar is display-only; seeking is driven by the player key handler (LEFT/RIGHT),
 * so no focus is required and the bar can never trap D-pad focus.
 */
@Composable
fun PlayerSeekBar(
    positionMs: Long,
    durationMs: Long,
    bufferedMs: Long,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false
) {
    val trackHeight by animateDpAsState(
        targetValue = if (emphasized) 8.dp else 6.dp,
        label = "seekBarTrackHeight"
    )
    val thumbSize by animateDpAsState(
        targetValue = if (emphasized) 16.dp else 10.dp,
        label = "seekBarThumbSize"
    )
    val playedFraction = fractionOf(positionMs, durationMs)
    val bufferedFraction = fractionOf(bufferedMs, durationMs)

    Box(
        modifier = modifier.height(20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .clip(SeekTrackShape)
                .background(DaiflixColors.DarkGray)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(bufferedFraction)
                .height(trackHeight)
                .clip(SeekTrackShape)
                .background(DaiflixColors.MediumGray)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(playedFraction)
                .height(trackHeight)
                .clip(SeekTrackShape)
                .background(DaiflixColors.White)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(playedFraction)
                .height(thumbSize),
            contentAlignment = Alignment.CenterEnd
        ) {
            Box(
                modifier = Modifier
                    .size(thumbSize)
                    .clip(CircleShape)
                    .background(DaiflixColors.White)
            )
        }
    }
}
