package com.yzion.daiflix.tv.ui.screen

import android.os.SystemClock
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.yzion.daiflix.tv.data.model.Danmaku
import com.yzion.daiflix.tv.ui.component.player.EpisodePanel
import com.yzion.daiflix.tv.ui.component.player.PlayerBottomBar
import com.yzion.daiflix.tv.ui.component.player.PlayerControl
import com.yzion.daiflix.tv.ui.component.player.PlayerLoadingOverlay
import com.yzion.daiflix.tv.ui.component.player.PlayerPausedOverlay
import com.yzion.daiflix.tv.ui.component.player.PlayerSeekFeedback
import com.yzion.daiflix.tv.ui.component.player.PlayerTopBar
import com.yzion.daiflix.tv.ui.component.player.SourcePanel
import com.yzion.daiflix.tv.ui.component.player.nextPlayerSpeed
import com.yzion.daiflix.tv.ui.theme.DaiflixColors
import com.yzion.daiflix.tv.ui.theme.DaiflixSpacing
import com.yzion.daiflix.tv.ui.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay

private const val SEEK_STEP_MS = 10_000L

/**
 * Hold-to-accelerate steps for LEFT/RIGHT while the HUD is hidden (audit P3-1):
 * 10s -> 30s -> 60s -> 300s. A press counts as a continuation of the running gesture
 * when it arrives within [SEEK_GESTURE_GAP_MS]; otherwise the gesture restarts at 10s,
 * which also implements "release / pause and the step resets".
 */
internal val PLAYER_SEEK_STEPS_MS = longArrayOf(10_000L, 30_000L, 60_000L, 300_000L)
private const val SEEK_GESTURE_GAP_MS = 500L
private const val HUD_AUTO_HIDE_MS = 4_000L

/** Which bottom sheet is currently open above the player. */
private enum class PlayerPanel { EPISODES, SOURCES }

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    vodId: String,
    sourceIndex: Int,
    episodeIndex: Int,
    onBack: () -> Unit,
    onEpisodeChange: (String, Int, Int) -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    // ------------------------------------------------------------------ HUD state
    var hudVisible by remember { mutableStateOf(true) }
    var selectedControl by remember { mutableStateOf(PlayerControl.PLAY) }
    var panel by remember { mutableStateOf<PlayerPanel?>(null) }
    var seekFeedback by remember { mutableStateOf<String?>(null) }
    var seekFeedbackTick by remember { mutableIntStateOf(0) }
    var interactionTick by remember { mutableIntStateOf(0) }
    // Hold-to-accelerate seek gesture state (audit P3-1).
    var seekStepIndex by remember { mutableIntStateOf(0) }
    var lastSeekDirection by remember { mutableIntStateOf(0) }
    var lastSeekAtMs by remember { mutableStateOf(0L) }
    val rootFocus = remember { FocusRequester() }

    // ------------------------------------------------------------------ player setup
    LaunchedEffect(uiState.playUrl, uiState.playbackKey) {
        val url = uiState.playUrl ?: return@LaunchedEffect
        val item = MediaItem.fromUri(url)
        val startPosition = uiState.resumePosition.coerceAtLeast(0)
        exoPlayer.setMediaItem(item, startPosition)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        // P2-9: only now may ExoPlayer's timeline drive the seek bar / time labels;
        // before this point the view-model keeps them at 0 for the new episode.
        viewModel.onMediaItemAttached()
    }

    LaunchedEffect(uiState.playbackSpeed) {
        exoPlayer.setPlaybackSpeed(uiState.playbackSpeed)
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED && viewModel.uiState.value.autoPlayNext) {
                    viewModel.playNextEpisode()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                viewModel.reportPlaybackError(
                    error.localizedMessage ?: "视频加载失败，请检查网络后重试",
                    exoPlayer.currentPosition
                )
            }
        }
        exoPlayer.addListener(listener)
        viewModel.attachPlayer(exoPlayer)
        onDispose {
            viewModel.saveProgressOnExit(exoPlayer.currentPosition, exoPlayer.duration)
            viewModel.detachPlayer()
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Save progress every 5 seconds while actually playing.
    LaunchedEffect(uiState.playUrl) {
        if (uiState.playUrl == null) return@LaunchedEffect
        while (true) {
            delay(5_000)
            if (exoPlayer.isPlaying) {
                viewModel.saveProgress(exoPlayer.currentPosition, exoPlayer.duration)
            }
        }
    }

    // ------------------------------------------------------------------ helpers
    fun leavePlayer() {
        viewModel.saveProgressOnExit(exoPlayer.currentPosition, exoPlayer.duration)
        onBack()
    }

    fun showHud() {
        hudVisible = true
        interactionTick++
    }

    fun performSeek(deltaMs: Long) {
        viewModel.seekBy(deltaMs)
        val seconds = (kotlin.math.abs(deltaMs) / 1000).coerceAtLeast(1)
        seekFeedback = if (deltaMs < 0) "快退 $seconds 秒" else "快进 $seconds 秒"
        seekFeedbackTick++
    }

    /**
     * Hold-to-accelerate seek (audit P3-1). Each press arriving within
     * [SEEK_GESTURE_GAP_MS] of the previous one advances the step
     * 10s -> 30s -> 60s -> 300s; a longer pause (release / pause) resets to 10s.
     *
     * The HUD stays hidden while seeking on purpose: if it became visible, the
     * following key repeats would take the HUD-visible LEFT/RIGHT branch and move
     * the control selection instead of seeking.
     */
    fun seekWithHold(direction: Int) {
        val now = SystemClock.uptimeMillis()
        val continuing = direction == lastSeekDirection &&
            now - lastSeekAtMs <= SEEK_GESTURE_GAP_MS
        seekStepIndex = nextSeekStepIndex(seekStepIndex, continuing)
        lastSeekDirection = direction
        lastSeekAtMs = now
        performSeek(direction * PLAYER_SEEK_STEPS_MS[seekStepIndex])
    }

    fun activateControl(control: PlayerControl) {
        when (control) {
            PlayerControl.REWIND -> performSeek(-SEEK_STEP_MS)
            PlayerControl.PLAY -> viewModel.togglePlay()
            PlayerControl.FORWARD -> performSeek(SEEK_STEP_MS)
            PlayerControl.NEXT -> {
                viewModel.playNextEpisode()
                hudVisible = true
            }
            PlayerControl.EPISODES -> {
                panel = PlayerPanel.EPISODES
                hudVisible = true
            }
            PlayerControl.SOURCES -> {
                panel = PlayerPanel.SOURCES
                hudVisible = true
            }
            PlayerControl.DANMAKU -> viewModel.toggleDanmaku()
            PlayerControl.SPEED -> viewModel.setSpeed(nextPlayerSpeed(uiState.playbackSpeed))
        }
    }

    fun handleKey(event: KeyEvent): Boolean {
        if (event.type != KeyEventType.KeyDown) return false
        interactionTick++

        // BACK is decided here and only here — this preview handler is the single
        // BACK path of the player (audit P1-4). The `BackHandler { ... }` that used to
        // sit at the end of this composable dispatched the *same* physical press
        // independently (OnBackPressedDispatcher) and called leavePlayer() again, so
        // one press ran popBackStack twice: player -> detail -> list. It was the
        // duplicate that got deleted (not this handler) because the panel / error
        // branches below must run before the normal HUD/exit logic, and only this
        // handler can express them.
        if (event.key == Key.Back) {
            when (playerBackAction(
                panelOpen = panel != null,
                hasError = uiState.error != null,
                hudVisible = hudVisible
            )) {
                // Panel open: consume BACK by closing the panel, never touch the back stack.
                PlayerBackAction.CLOSE_PANEL -> panel = null
                // Normal playback: first BACK only dismisses the HUD.
                PlayerBackAction.HIDE_HUD -> hudVisible = false
                // Error state / normal playback with the HUD hidden: leave exactly one level.
                PlayerBackAction.EXIT_PLAYER -> leavePlayer()
            }
            return true
        }

        // A panel owns the D-pad for every other key.
        if (panel != null) return false

        // The error screen keeps its own focus handling for every other key.
        if (uiState.error != null) return false

        when (event.key) {
            Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                if (hudVisible) {
                    activateControl(selectedControl)
                } else {
                    // Spec: OK with the HUD hidden toggles play/pause (and reveals the HUD).
                    viewModel.togglePlay()
                    showHud()
                }
                return true
            }
            Key.DirectionUp -> {
                if (!hudVisible) {
                    showHud()
                } else {
                    selectedControl = previousControl(selectedControl)
                }
                return true
            }
            Key.DirectionDown -> {
                if (!hudVisible) {
                    showHud()
                } else {
                    selectedControl = nextControl(selectedControl)
                }
                return true
            }
            // HUD hidden: LEFT/RIGHT is the quick seek gesture, accelerated when held
            // (10s -> 30s -> 60s -> 300s, P3-1). The HUD is intentionally not revealed
            // here so further key repeats keep seeking instead of moving the selection.
            // HUD visible: LEFT/RIGHT moves the control selection instead, so every
            // control (episodes/sources/speed) stays reachable from the D-pad; the
            // dedicated 快退/快进 buttons perform a ±10s seek with OK.
            Key.DirectionLeft -> {
                if (!hudVisible) {
                    seekWithHold(-1)
                } else {
                    selectedControl = previousControl(selectedControl)
                }
                return true
            }
            Key.DirectionRight -> {
                if (!hudVisible) {
                    seekWithHold(1)
                } else {
                    selectedControl = nextControl(selectedControl)
                }
                return true
            }
            Key.MediaPlayPause, Key.MediaPlay, Key.MediaPause -> {
                viewModel.togglePlay()
                showHud()
                return true
            }
        }
        return false
    }

    // NOTE: there is deliberately no BackHandler here. A BackHandler + the root
    // onPreviewKeyEvent below both fired for one BACK press, which doubled the
    // popBackStack (audit P1-4). BACK is handled exclusively in handleKey() above.

    // Keep the invisible root anchor focused so key handling always works.
    LaunchedEffect(panel, uiState.error) {
        if (panel == null && uiState.error == null) {
            delay(80)
            runCatching { rootFocus.requestFocus() }
        }
    }

    // Auto-hide the HUD after 4 seconds of inactivity while playing.
    LaunchedEffect(hudVisible, uiState.isPlaying, uiState.isBuffering, panel, interactionTick) {
        if (!hudVisible || panel != null) return@LaunchedEffect
        delay(HUD_AUTO_HIDE_MS)
        if (panel == null && uiState.isPlaying && !uiState.isBuffering && uiState.error == null) {
            hudVisible = false
        }
    }

    // Clear the seek feedback pill shortly after it appears.
    LaunchedEffect(seekFeedbackTick) {
        if (seekFeedback != null) {
            delay(900)
            seekFeedback = null
        }
    }

    // ------------------------------------------------------------------ layout
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DaiflixColors.Black)
            .focusRequester(rootFocus)
            .onPreviewKeyEvent { handleKey(it) }
            .focusable()
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    setBackgroundColor(android.graphics.Color.BLACK)
                    isFocusable = false
                    isFocusableInTouchMode = false
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            update = { view -> view.player = exoPlayer },
            modifier = Modifier.fillMaxSize()
        )

        if (uiState.danmakuEnabled && uiState.danmakuList.isNotEmpty()) {
            DanmakuOverlay(
                danmakuList = uiState.danmakuList,
                currentPosition = { exoPlayer.currentPosition / 1000f },
                opacity = uiState.danmakuOpacity,
                sizeScale = uiState.danmakuSize,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Paused overlay.
        if (uiState.playUrl != null && !uiState.isPlaying && !uiState.isBuffering &&
            !uiState.isLoading && uiState.error == null
        ) {
            PlayerPausedOverlay()
        }

        // Seek feedback ("快退 10 秒" / "快进 10 秒").
        seekFeedback?.let { text ->
            PlayerSeekFeedback(text = text)
        }

        // Custom HUD.
        AnimatedVisibility(
            visible = hudVisible && panel == null && uiState.error == null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                PlayerTopBar(
                    title = uiState.vodTitle.ifBlank { uiState.title },
                    episodeLabel = currentEpisodeLabel(uiState.currentEpisode?.name, uiState.episodeIndex),
                    sourceLabel = uiState.currentSource?.name.orEmpty(),
                    onBack = { leavePlayer() },
                    modifier = Modifier.align(Alignment.TopCenter)
                )
                PlayerBottomBar(
                    positionMs = uiState.positionMs,
                    durationMs = uiState.durationMs,
                    bufferedMs = uiState.bufferedMs,
                    isPlaying = uiState.isPlaying,
                    speed = uiState.playbackSpeed,
                    danmakuEnabled = uiState.danmakuEnabled,
                    danmakuCount = uiState.danmakuList.size,
                    hasNext = uiState.hasNext,
                    selected = selectedControl,
                    onControlClick = { control ->
                        selectedControl = control
                        activateControl(control)
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

        // Loading overlay (audit P2-9). One overlay covers both resolving the play URL
        // and buffering / switching episodes, and it names the title and episode so the
        // user never sees an unexplained black screen. The time axis stays at 0 until
        // the new media is attached (see PlayerViewModel.onMediaItemAttached()).
        if (uiState.error == null &&
            (uiState.isBuffering || (uiState.playUrl == null && uiState.isLoading))
        ) {
            PlayerLoadingOverlay(
                label = playerLoadingLabel(
                    vodTitle = uiState.vodTitle,
                    episodeName = uiState.currentEpisode?.name,
                    episodeIndex = uiState.episodeIndex
                )
            )
        }

        // Episode / source bottom sheets.
        when (panel) {
            PlayerPanel.EPISODES -> EpisodePanel(
                episodes = uiState.episodes,
                currentIndex = uiState.episodeIndex,
                sourceName = uiState.currentSource?.name.orEmpty(),
                onSelect = { index ->
                    panel = null
                    viewModel.selectEpisode(index)
                },
                onClose = { panel = null }
            )
            PlayerPanel.SOURCES -> SourcePanel(
                sources = uiState.sources,
                currentIndex = uiState.sourceIndex,
                onSelect = { index ->
                    panel = null
                    viewModel.selectSource(index)
                },
                onClose = { panel = null }
            )
            null -> Unit
        }

        // Error panel keeps its retry logic.
        uiState.error?.let { message ->
            PlaybackErrorPanel(
                message = message,
                title = uiState.title,
                onRetry = { viewModel.retry() },
                onBack = { leavePlayer() }
            )
        }
    }
}

private fun nextControl(control: PlayerControl): PlayerControl {
    val values = PlayerControl.entries
    return values[(values.indexOf(control) + 1) % values.size]
}

private fun previousControl(control: PlayerControl): PlayerControl {
    val values = PlayerControl.entries
    return values[(values.indexOf(control) - 1 + values.size) % values.size]
}

private fun currentEpisodeLabel(episodeName: String?, episodeIndex: Int): String = when {
    !episodeName.isNullOrBlank() -> episodeName
    else -> "第${episodeIndex + 1}集"
}

/**
 * Advances the hold-to-accelerate seek step (audit P3-1). [continuing] is true when the
 * press belongs to the running gesture: the step then moves up one and clamps at the
 * last (300s) step. Otherwise the gesture restarts at 10s.
 */
internal fun nextSeekStepIndex(current: Int, continuing: Boolean): Int =
    if (!continuing) 0 else (current + 1).coerceAtMost(PLAYER_SEEK_STEPS_MS.lastIndex)

/**
 * Label of the player loading overlay (audit P2-9): "正在加载 · 剧名 第N集", or without
 * the title when the state does not carry one: "正在加载 · 第N集".
 */
internal fun playerLoadingLabel(vodTitle: String, episodeName: String?, episodeIndex: Int): String {
    val episode = currentEpisodeLabel(episodeName, episodeIndex)
    val title = vodTitle.trim()
    return if (title.isEmpty()) "正在加载 · $episode" else "正在加载 · $title $episode"
}

/** What a single BACK press on the player screen must do. */
internal enum class PlayerBackAction { CLOSE_PANEL, HIDE_HUD, EXIT_PLAYER }

/**
 * The one and only BACK decision of [PlayerScreen], kept pure so the
 * panel / error / normal semantics are unit-testable without Compose
 * (audit P1-4: the screen used to have two BACK paths that each popped the back stack).
 *
 * - panel open   -> only close the panel, never touch the back stack;
 * - error state  -> leave the player exactly one level (back to detail);
 * - normal       -> dismiss the HUD first; the next BACK leaves one level.
 */
internal fun playerBackAction(
    panelOpen: Boolean,
    hasError: Boolean,
    hudVisible: Boolean
): PlayerBackAction = when {
    panelOpen -> PlayerBackAction.CLOSE_PANEL
    hasError -> PlayerBackAction.EXIT_PLAYER
    hudVisible -> PlayerBackAction.HIDE_HUD
    else -> PlayerBackAction.EXIT_PLAYER
}

@Composable
private fun PlaybackErrorPanel(
    message: String,
    title: String,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    val retryFocus = remember { FocusRequester() }
    LaunchedEffect(message) {
        delay(120)
        runCatching { retryFocus.requestFocus() }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DaiflixColors.Black.copy(alpha = 0.86f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "播放失败",
                style = MaterialTheme.typography.titleLarge,
                color = DaiflixColors.Red
            )
            Spacer(Modifier.height(DaiflixSpacing.xs))
            if (title.isNotBlank()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DaiflixColors.LightGray
                )
            }
            Spacer(Modifier.height(DaiflixSpacing.xs))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = DaiflixColors.MediumGray
            )
            Spacer(Modifier.height(DaiflixSpacing.md))
            Row(horizontalArrangement = Arrangement.spacedBy(DaiflixSpacing.sm)) {
                Button(onClick = onRetry, modifier = Modifier.focusRequester(retryFocus)) {
                    Text("重试")
                }
                Button(onClick = onBack) {
                    Text("返回")
                }
            }
        }
    }
}

@Composable
private fun DanmakuOverlay(
    danmakuList: List<Danmaku>,
    currentPosition: () -> Float,
    opacity: Float,
    sizeScale: Float,
    modifier: Modifier = Modifier
) {
    var now by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            now = currentPosition()
            delay(100)
        }
    }
    val visible = danmakuList.filter {
        val age = now - it.time
        age in 0f..8f
    }
    BoxWithConstraints(modifier = modifier) {
        visible.take(24).forEachIndexed { index, danmaku ->
            val age = (now - danmaku.time).coerceIn(0f, 8f)
            val progress = age / 8f
            Text(
                text = danmaku.text,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp * sizeScale),
                color = Color(0xFF000000.toInt() or danmaku.color)
                    .copy(alpha = opacity * (1f - progress * 0.7f)),
                modifier = Modifier
                    .offset(
                        x = (maxWidth + 40.dp) * (1f - progress) - 40.dp,
                        y = (56 + index * 34).dp
                    )
                    .alpha(1f - progress * 0.85f)
            )
        }
    }
}

