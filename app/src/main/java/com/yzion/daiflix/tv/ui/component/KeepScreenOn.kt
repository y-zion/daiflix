package com.yzion.daiflix.tv.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

/**
 * Keeps the TV panel awake for as long as this composable is on screen.
 *
 * `View.keepScreenOn = true` is the Android-blessed replacement for holding a
 * `SCREEN_BRIGHT_WAKE_LOCK`: the platform translates it into the window flag
 * [android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON], so the screen
 * stays lit while it is set and there is no wake lock to leak when the process
 * is killed. It is scoped to the player route only — every other screen is
 * still allowed to idle into the screensaver.
 *
 * The previous value is restored on dispose so leaving the player immediately
 * hands screen-timeout control back to the system.
 */
@Composable
fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        val previous = view.keepScreenOn
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = previous }
    }
}
