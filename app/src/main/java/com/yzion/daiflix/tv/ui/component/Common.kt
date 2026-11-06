package com.yzion.daiflix.tv.ui.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.yzion.daiflix.tv.ui.theme.DaiflixColors
import com.yzion.daiflix.tv.ui.theme.DaiflixSpacing

/**
 * Playful-but-not-loud focus ring shared by interactive surfaces. The ring is
 * drawn inset over the content, matching Netflix's white outline on TVs.
 */
fun focusBorder(color: Color = DaiflixColors.FocusRing): Border =
    Border(BorderStroke(2.dp, color))

/** Netflix "Play" button: solid white, black label, grows and lifts on focus. */
@Composable
fun DaiflixPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Surface(
        onClick = onClick,
        interactionSource = interaction,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(6.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = DaiflixColors.White,
            contentColor = DaiflixColors.Black,
            focusedContainerColor = DaiflixColors.White,
            focusedContentColor = DaiflixColors.Black
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = DaiflixColors.Black
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (focused) DaiflixColors.Black else DaiflixColors.Black
            )
        }
    }
}

/** Netflix secondary button: translucent grey, turns white with black label on focus. */
@Composable
fun DaiflixSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Surface(
        onClick = onClick,
        interactionSource = interaction,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(6.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0xB36D6D70),
            contentColor = DaiflixColors.White,
            focusedContainerColor = DaiflixColors.White,
            focusedContentColor = DaiflixColors.Black
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = if (focused) DaiflixColors.Black else DaiflixColors.White
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (focused) DaiflixColors.Black else DaiflixColors.White
            )
        }
    }
}

/** Small translucent metadata pill ("HD", "16+", "2024"). */
@Composable
fun MetaPill(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = DaiflixColors.LightGray
) {
    Box(
        modifier = modifier
            .background(Color(0x33000000), RoundedCornerShape(3.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color
        )
    }
}

/** Row/section title used above every carousel. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DaiflixSpacing.screenPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = DaiflixColors.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        trailing?.invoke()
    }
}

/** Left-to-right black fade used to keep hero copy readable over artwork. */
@Composable
fun HorizontalScrim(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            Brush.horizontalGradient(
                0f to DaiflixColors.Black.copy(alpha = 0.92f),
                0.45f to DaiflixColors.Black.copy(alpha = 0.55f),
                1f to Color.Transparent
            )
        )
    )
}

/** Bottom-up black fade used under hero copy and the player control bar. */
@Composable
fun BottomScrim(modifier: Modifier = Modifier, strength: Float = 0.95f) {
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                0f to Color.Transparent,
                0.55f to DaiflixColors.Black.copy(alpha = strength * 0.45f),
                1f to DaiflixColors.Black.copy(alpha = strength)
            )
        )
    )
}

/** Top-down fade so the nav bar blends into artwork. */
@Composable
fun TopScrim(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                0f to DaiflixColors.Black.copy(alpha = 0.9f),
                1f to Color.Transparent
            )
        )
    )
}

/** Simple branded loading state. */
@Composable
fun LoadingState(
    modifier: Modifier = Modifier,
    message: String = "加载中…"
) {
    val transition = rememberInfiniteTransition(label = "loading")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "loading-alpha"
    )
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            DaiflixMark(height = 56.dp, modifier = Modifier.alpha(alpha))
            Spacer(modifier = Modifier.height(DaiflixSpacing.md))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = DaiflixColors.LightGray
            )
        }
    }
}

/** Centered empty/error message with an optional action. */
@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = DaiflixSpacing.xxl)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = DaiflixColors.White
            )
            if (message != null) {
                Spacer(modifier = Modifier.height(DaiflixSpacing.sm))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = DaiflixColors.LightGray
                )
            }
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(DaiflixSpacing.lg))
                DaiflixPrimaryButton(text = actionLabel, onClick = onAction)
            }
        }
    }
}
