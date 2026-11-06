package com.y_zion.daiflix.ui.screen

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.y_zion.daiflix.ui.theme.DaiflixColors
import com.y_zion.daiflix.ui.theme.DaiflixSpacing
import com.y_zion.daiflix.ui.viewmodel.PlayerViewModel

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

    // ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    // 播放 URL 变化时设置媒体
    LaunchedEffect(uiState.playUrl) {
        val url = uiState.playUrl
        if (!url.isNullOrBlank()) {
            val mediaItem = MediaItem.fromUri(url)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
        }
    }

    // 监听播放完成
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    viewModel.playNextEpisode()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                // 播放错误处理
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            // 保存进度
            viewModel.saveHistory(
                position = exoPlayer.currentPosition,
                duration = exoPlayer.duration
            )
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DaiflixColors.Black)
    ) {
        if (uiState.isLoading) {
            // 加载中
            Box(
                modifier = Modifier.fillMaxSize(),
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
        } else if (uiState.error != null) {
            // 错误
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "播放失败",
                        style = MaterialTheme.typography.titleLarge,
                        color = DaiflixColors.Red
                    )
                    Spacer(modifier = Modifier.height(DaiflixSpacing.xs))
                    Text(
                        text = uiState.error ?: "未知错误",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DaiflixColors.LightGray
                    )
                    Spacer(modifier = Modifier.height(DaiflixSpacing.md))
                    Text(
                        text = "按返回键退出",
                        style = MaterialTheme.typography.bodySmall,
                        color = DaiflixColors.MediumGray
                    )
                }
            }
        } else {
            // 播放器视图
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
                        // 16:9 全屏
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // 弹幕层（简化版，后续可增强）
            if (uiState.danmakuEnabled && uiState.danmakuList.isNotEmpty()) {
                DanmakuOverlay(
                    danmakuList = uiState.danmakuList,
                    currentPosition = { exoPlayer.currentPosition / 1000f },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * 简易弹幕覆盖层
 */
@Composable
private fun DanmakuOverlay(
    danmakuList: List<com.y_zion.daiflix.data.model.Danmaku>,
    currentPosition: () -> Float,
    modifier: Modifier = Modifier
) {
    // 简化实现：只显示当前时间附近的弹幕
    val currentTime = currentPosition()
    val visibleDanmaku = danmakuList.filter {
        it.time in (currentTime - 5f)..currentTime
    }

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            visibleDanmaku.take(5).forEach { dm ->
                Text(
                    text = dm.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = DaiflixColors.White.copy(alpha = 0.8f),
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .background(
                            DaiflixColors.Black.copy(alpha = 0.3f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
