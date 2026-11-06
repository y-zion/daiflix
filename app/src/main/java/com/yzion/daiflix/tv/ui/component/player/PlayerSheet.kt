package com.yzion.daiflix.tv.ui.component.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.yzion.daiflix.tv.ui.theme.DaiflixColors
import com.yzion.daiflix.tv.ui.theme.DaiflixSpacing
import kotlinx.coroutines.delay

/**
 * Bottom-sheet scaffold used by the episode and source panels.
 * The screen composes it only while a panel is open, so the enter animation is
 * driven by a local flag flipped right after the first composition.
 */
@Composable
fun PlayerSheet(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(16)
        shown = true
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DaiflixColors.ScrimStrong)
    ) {
        AnimatedVisibility(
            visible = shown,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = DaiflixColors.DarkSurface,
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                    )
                    .padding(
                        start = DaiflixSpacing.screenPadding,
                        end = DaiflixSpacing.screenPadding,
                        top = DaiflixSpacing.lg,
                        bottom = DaiflixSpacing.xl
                    )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = DaiflixColors.White
                    )
                    Text(
                        text = "按返回键关闭",
                        style = MaterialTheme.typography.bodySmall,
                        color = DaiflixColors.TextTertiary
                    )
                }
                Spacer(Modifier.height(DaiflixSpacing.md))
                content()
            }
        }
    }
}

/** Dim scrim drawn behind the HUD while a panel is open. */
@Composable
fun PlayerPanelScrim(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)))
}
