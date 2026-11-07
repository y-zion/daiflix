package com.yzion.daiflix.tv.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yzion.daiflix.tv.ui.theme.DaiflixColors

/**
 * TV 焦点态，统一由 [Modifier.onFocusChanged] 驱动。
 *
 * 为什么不用 tv-material3 的 `collectIsFocusedAsState()`：它内部在
 * `LaunchedEffect` 里订阅 `MutableInteractionSource.interactions`，而这个
 * Flow **不重放**事件。首屏抢焦点（`LaunchedEffect { focusRequester.requestFocus() }`）
 * 时，父级 composable 的 effect 先于子级组件的订阅 effect 执行，第一个
 * `FocusInteraction.Focus` 事件被丢掉 —— 于是出现「元素确实拿到了焦点，
 * 但焦点样式要按一下方向键才出现」的问题（审计 P1-3）。
 *
 * `onFocusChanged` 在焦点状态变化时同步回调，与订阅时序无关，因此用它来驱动
 * 所有焦点样式。用法：
 *
 * ```
 * val focus = rememberFocusState()
 * Surface(
 *     modifier = modifier.trackFocus(focus).focusHalo(focus),
 *     ...
 * )
 * ```
 */
@Stable
class FocusState {
    var isFocused by mutableStateOf(false)
        internal set
}

@Composable
fun rememberFocusState(): FocusState = remember { FocusState() }

/**
 * 把焦点状态写进 [state]。必须放在 `clickable` / `focusable` **之前**的
 * modifier 链位置（也就是传给 Surface 的 modifier 上），这样它观察到的才是
 * 组件自身的焦点目标。
 */
fun Modifier.trackFocus(state: FocusState): Modifier =
    this.onFocusChanged { state.isFocused = it.isFocused }

/**
 * 画在组件外沿的焦点环（白圈 + 3dp 暗色间隙），用于白底/深底都需要的按钮。
 * 环形绘制在布局边界之外，不改变尺寸，因此不会让按钮在获得焦点时抖动。
 *
 * 注意：外沿 3dp 会画在布局外，被 LazyRow/LazyGrid 的滚动裁剪时可能被裁掉。
 * 列表项请改用 [focusBorder]（内描边）。
 */
fun Modifier.focusHalo(
    state: FocusState,
    cornerRadius: Dp = 8.dp,
    color: Color = DaiflixColors.FocusRing,
    inset: Dp = 3.dp,
    width: Dp = 2.dp
): Modifier = this.drawWithContent {
    drawContent()
    if (state.isFocused) {
        val grow = inset.toPx()
        val strokeWidth = width.toPx()
        drawRoundRect(
            color = color,
            topLeft = Offset(-grow, -grow),
            size = Size(size.width + grow * 2, size.height + grow * 2),
            cornerRadius = CornerRadius(cornerRadius.toPx() + grow),
            style = Stroke(width = strokeWidth)
        )
    }
}

/**
 * 内描边焦点边框（画在组件内部，与 [focusBorder] 视觉一致），由 [state] 驱动。
 * 未聚焦时画透明边框，保持尺寸稳定。
 */
fun Modifier.insetFocusBorder(
    state: FocusState,
    width: Dp = 2.dp,
    color: Color = DaiflixColors.FocusRing
): Modifier = this.drawWithContent {
    drawContent()
    if (state.isFocused) {
        val strokeWidth = width.toPx()
        drawRoundRect(
            color = color,
            topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
            size = Size(size.width - strokeWidth, size.height - strokeWidth),
            cornerRadius = CornerRadius(8.dp.toPx()),
            style = Stroke(width = strokeWidth)
        )
    }
}

/**
 * 首帧之后再请求焦点。
 *
 * 直接在 `LaunchedEffect(Unit)` 里 `requestFocus()` 会赶在子组件的
 * `collectIsFocusedAsState` 订阅之前触发（父 effect 先执行），首个 Focus 事件
 * 丢失。延后一帧可以确保所有子组件的订阅与 focus target 都已就绪，同时
 * 保留 tv-material 自带的 focused 配色（不只依赖 [rememberFocusState]）。
 */
@Composable
fun RequestFocusWhenReady(
    focusRequester: FocusRequester,
    key: Any? = Unit,
    enabled: Boolean = true
) {
    LaunchedEffect(focusRequester, key, enabled) {
        if (!enabled) return@LaunchedEffect
        // 作用域里所有 LaunchedEffect 都在同一帧前被派发完，等一帧即可。
        withFrameNanos { }
        runCatching { focusRequester.requestFocus() }
    }
}
