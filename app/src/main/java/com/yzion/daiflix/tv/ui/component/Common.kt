package com.yzion.daiflix.tv.ui.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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

/**
 * Netflix "Play" button: solid white, black label.
 *
 * 焦点态 = 品牌红底白字 + 1.06 放大，不再画组件外的白色焦点环
 * （2026-11-07 反馈：白圈看着像 bug，只要放大/变色即可）。
 */
@Composable
fun DaiflixPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val focus = rememberFocusState()
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(6.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = DaiflixColors.White,
            contentColor = DaiflixColors.Black,
            focusedContainerColor = DaiflixColors.Red,
            focusedContentColor = DaiflixColors.White
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
        modifier = modifier.trackFocus(focus)
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
                    tint = if (focus.isFocused) DaiflixColors.White else DaiflixColors.Black
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (focus.isFocused) DaiflixColors.White else DaiflixColors.Black
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
    val focus = rememberFocusState()
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(6.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0xB36D6D70),
            contentColor = DaiflixColors.White,
            focusedContainerColor = DaiflixColors.White,
            focusedContentColor = DaiflixColors.Black
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
        modifier = modifier.trackFocus(focus)
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
                    tint = if (focus.isFocused) DaiflixColors.Black else DaiflixColors.White
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (focus.isFocused) DaiflixColors.Black else DaiflixColors.White
            )
        }
    }
}

/**
 * Destructive action button (审计 P1-7)：红描边 + 红字，焦点时红底白字。
 * 与主按钮的白底形成明确的危险/普通语义区分；不使用外沿焦点环。
 */
@Composable
fun DaiflixDangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val focus = rememberFocusState()
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(6.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = DaiflixColors.Red,
            focusedContainerColor = DaiflixColors.Red,
            focusedContentColor = DaiflixColors.White
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(2.dp, DaiflixColors.Red)),
            focusedBorder = Border.None
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
        modifier = modifier.trackFocus(focus)
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
                    tint = if (focus.isFocused) DaiflixColors.White else DaiflixColors.Red
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (focus.isFocused) DaiflixColors.White else DaiflixColors.Red
            )
        }
    }
}

/** Small translucent metadata pill ("HD", "16+", "2024"). */
/**
 * Shared circular back button for every sub-screen header (audit P3-4: the detail page
 * used "black 55% + white arrow" while history / favorites / search used "white + black
 * arrow"). One style everywhere: a translucent black disc that stays legible on top of
 * artwork, plus a 1dp hairline so it is still visible on the near-black page background.
 */
@Composable
fun DaiflixBackButton(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null
) {
    val focus = rememberFocusState()
    Surface(
        onClick = onBack,
        modifier = modifier
            .size(48.dp)
            .then(
                if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier
            )
            .trackFocus(focus),
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        // Colors are driven by focus.isFocused (not the tv-material interaction source), so the
        // focused white disc renders on the very first frame (audit P1-3).
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (focus.isFocused) DaiflixColors.White else DaiflixColors.Black.copy(alpha = 0.55f),
            contentColor = if (focus.isFocused) DaiflixColors.Black else DaiflixColors.White,
            focusedContainerColor = if (focus.isFocused) DaiflixColors.White else DaiflixColors.Black.copy(alpha = 0.55f),
            focusedContentColor = if (focus.isFocused) DaiflixColors.Black else DaiflixColors.White
        ),
        border = ClickableSurfaceDefaults.border(
            border = if (focus.isFocused) Border.None else Border(BorderStroke(1.dp, DaiflixColors.White.copy(alpha = 0.25f))),
            focusedBorder = Border.None
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Rounded.ArrowBack,
                contentDescription = "返回",
                modifier = Modifier.size(24.dp),
                tint = if (focus.isFocused) DaiflixColors.Black else DaiflixColors.White
            )
        }
    }
}

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
    onAction: (() -> Unit)? = null,
    /**
     * When supplied, the action button takes the initial D-pad focus. Empty screens
     * have nothing else focusable, so without this the remote would be dead until
     * the user found the one button by luck.
     */
    actionFocusRequester: FocusRequester? = null
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
                if (actionFocusRequester != null) {
                    RequestFocusWhenReady(actionFocusRequester)
                }
                DaiflixPrimaryButton(
                    text = actionLabel,
                    onClick = onAction,
                    modifier = if (actionFocusRequester != null) {
                        Modifier.focusRequester(actionFocusRequester)
                    } else {
                        Modifier
                    }
                )
            }
        }
    }
}
