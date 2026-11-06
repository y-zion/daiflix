package com.y_zion.daiflix.ui.screen

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
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
import com.y_zion.daiflix.data.model.Danmaku
import com.y_zion.daiflix.ui.theme.DaiflixColors
import com.y_zion.daiflix.ui.theme.DaiflixSpacing
import com.y_zion.daiflix.ui.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay

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
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val currentState by rememberUpdatedState(uiState)

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    // 播放 URL 变化时设置媒体；首次进入从历史位置续播
    LaunchedEffect(uiState.playUrl) {
        val url = uiState.playUrl
        if (!url.isNullOrBlank()) {
            exoPlayer.setMediaItem(
                MediaItem.fromUri(url),
                uiState.resumePosition.coerceAtLeast(0)
            )
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }
    }

    LaunchedEffect(uiState.playbackSpeed) {
        exoPlayer.setPlaybackSpeed(uiState.playbackSpeed)
    }

    // 播放结束：按设置自动下一集
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED && currentState.autoPlayNext) {
                    viewModel.playNextEpisode()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                viewModel.reportPlaybackError(
                    error.localizedMessage ?: "视频加载失败，请检查网络后重试",
                    position = exoPlayer.currentPosition
                )
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            viewModel.saveProgressOnExit(
                position = exoPlayer.currentPosition,
                duration = exoPlayer.duration
            )
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // 定时落盘，避免只在退出时保存（崩溃/断电会丢进度）
    LaunchedEffect(uiState.playUrl) {
        if (uiState.playUrl == null) return@LaunchedEffect
        while (true) {
            delay(5_000)
            if (exoPlayer.isPlaying) {
                viewModel.saveProgress(
                    position = exoPlayer.currentPosition,
                    duration = exoPlayer.duration
                )
            }
        }
    }

    BackHandler {
        viewModel.saveProgress(
            position = exoPlayer.currentPosition,
            duration = exoPlayer.duration
        )
        onBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DaiflixColors.Black)
    ) {
        if (uiState.playUrl != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                        setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                },
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
        }

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DaiflixColors.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "正在解析播放地址...",
                        style = MaterialTheme.typography.titleMedium,
                        color = DaiflixColors.White
                    )
                    Spacer(modifier = Modifier.height(DaiflixSpacing.sm))
                    Text(
                        text = uiState.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = DaiflixColors.LightGray
                    )
                }
            }
        }

        if (uiState.error != null && uiState.playUrl == null) {
            PlaybackErrorPanel(
                message = uiState.error ?: "未知错误",
                title = uiState.title,
                onRetry = { viewModel.retry() },
                onBack = onBack
            )
        }
    }
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
            Spacer(modifier = Modifier.height(DaiflixSpacing.xs))
            if (title.isNotBlank()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DaiflixColors.LightGray
                )
            }
            Spacer(modifier = Modifier.height(DaiflixSpacing.xs))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = DaiflixColors.MediumGray
            )
            Spacer(modifier = Modifier.height(DaiflixSpacing.md))
            Row(horizontalArrangement = Arrangement.spacedBy(DaiflixSpacing.sm)) {
                Button(
                    onClick = onRetry,
                    modifier = Modifier.focusRequester(retryFocus)
                ) {
                    Text("重试")
                }
                Button(onClick = onBack) {
                    Text("返回")
                }
            }
        }
    }
}

/**
 * 弹幕覆盖层：按播放时间推进，弹幕从右向左滚动。
 * 旧实现只在组合时取一次 currentPosition()，导致弹幕永远静止。
 */
@Composable
private fun DanmakuOverlay(
    danmakuList: List<Danmaku>,
    currentPosition: () -> Float,
    opacity: Float,
    sizeScale: Float,
    modifier: Modifier = Modifier
) {
    var now by remember { mutableFloatStateOf(0f) }

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
        visible.take(24).forEachIndexed { index, dm ->
            val age = (now - dm.time).coerceIn(0f, 8f)
            val progress = age / 8f
            Text(
                text = dm.text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 18.sp * sizeScale
                ),
                color = Color(0xFF000000.toInt() or dm.color)
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
