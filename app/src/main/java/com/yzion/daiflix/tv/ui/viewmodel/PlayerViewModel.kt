package com.yzion.daiflix.tv.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.yzion.daiflix.tv.data.local.AppPreferences
import com.yzion.daiflix.tv.data.model.*
import com.yzion.daiflix.tv.data.repository.VodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerUiState(
    val isLoading: Boolean = true,
    val playUrl: String? = null,
    val playType: PlayType = PlayType.HLS,
    val danmakuList: List<Danmaku> = emptyList(),
    val danmakuEnabled: Boolean = true,
    val currentEpisode: Episode? = null,
    val currentSource: PlaySource? = null,
    val sources: List<PlaySource> = emptyList(),
    val episodes: List<Episode> = emptyList(),
    val sourceIndex: Int = 0,
    val episodeIndex: Int = 0,
    /**
     * vodId:sourceIndex:episodeIndex of the episode the UI wants to show. Compared with
     * the media actually attached to ExoPlayer so a switch never leaks the previous
     * episode's position/duration into the new timeline (audit P2-9).
     */
    val playbackKey: String = "",
    val vodTitle: String = "",
    val title: String = "",
    val resumePosition: Long = 0,
    val hasNext: Boolean = false,
    val autoPlayNext: Boolean = true,
    val playbackSpeed: Float = 1f,
    val danmakuOpacity: Float = 0.8f,
    val danmakuSize: Float = 1f,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val bufferedMs: Long = 0,
    val error: String? = null
)

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val repository: VodRepository,
    private val preferences: AppPreferences,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val vodId: String = savedStateHandle["vodId"] ?: ""
    private var sourceIndex: Int = savedStateHandle["sourceIndex"] ?: 0
    private var episodeIndex: Int = savedStateHandle["episodeIndex"] ?: 0

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var detail: VodDetail? = null
    private var playJob: Job? = null
    private var history: WatchHistory? = null

    // The screen owns the ExoPlayer lifecycle; the ViewModel only observes and commands it.
    private var player: ExoPlayer? = null

    /** [PlayerUiState.playbackKey] of the media that is actually inside [player]. */
    private var mediaKey: String? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            refreshPlayback()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            refreshPlayback()
        }
    }

    init {
        _uiState.update {
            it.copy(
                danmakuEnabled = repository.isDanmakuEnabled(),
                autoPlayNext = repository.isAutoPlayNext(),
                playbackSpeed = repository.getPlaybackSpeed(),
                danmakuOpacity = repository.getDanmakuOpacity(),
                danmakuSize = repository.getDanmakuSize()
            )
        }
        loadPlayer()
        startPositionTicker()
    }

    // ---------------------------------------------------------------- player wiring

    /** Called by the screen once its ExoPlayer is created. */
    fun attachPlayer(exoPlayer: ExoPlayer) {
        if (player === exoPlayer) return
        player?.removeListener(playerListener)
        player = exoPlayer
        exoPlayer.addListener(playerListener)
        exoPlayer.setPlaybackSpeed(_uiState.value.playbackSpeed)
        refreshPlayback()
    }

    /**
     * Called by the screen right after it hands the resolved URL to ExoPlayer:
     * only from this point on may the player's timeline drive the seek bar.
     */
    fun onMediaItemAttached() {
        mediaKey = _uiState.value.playbackKey
        refreshPlayback()
    }

    /** Called by the screen before it releases the ExoPlayer. */
    fun detachPlayer() {
        player?.removeListener(playerListener)
        player = null
        mediaKey = null
        _uiState.update { it.copy(isPlaying = false, isBuffering = false) }
    }

    private fun startPositionTicker() {
        viewModelScope.launch {
            while (true) {
                refreshPlayback()
                delay(500)
            }
        }
    }

    private fun refreshPlayback() {
        val p = player ?: return
        val state = _uiState.value
        // While a switch is in flight ExoPlayer still holds the previous episode; its
        // position/duration must not leak into the new episode's timeline (audit P2-9),
        // so the timeline stays at 0 until the screen reports the new media attached.
        val mediaReady = state.playbackKey.isNotEmpty() && state.playbackKey == mediaKey
        val duration = p.duration
        _uiState.update {
            it.copy(
                isPlaying = p.isPlaying,
                isBuffering = p.playbackState == Player.STATE_BUFFERING,
                positionMs = if (mediaReady) p.currentPosition.coerceAtLeast(0) else 0L,
                durationMs = if (mediaReady && duration > 0) duration else 0L,
                bufferedMs = if (mediaReady) p.bufferedPosition.coerceAtLeast(0) else 0L
            )
        }
    }

    // ---------------------------------------------------------------- transport commands

    fun togglePlay() {
        val p = player ?: return
        if (p.playbackState == Player.STATE_ENDED) {
            p.seekTo(0)
            p.play()
        } else if (p.isPlaying) {
            p.pause()
        } else {
            p.play()
        }
        refreshPlayback()
    }

    fun seekBy(deltaMs: Long) {
        seekTo(_uiState.value.positionMs + deltaMs)
    }

    fun seekTo(positionMs: Long) {
        val p = player ?: return
        val duration = _uiState.value.durationMs
        val upper = if (duration > 0) duration else Long.MAX_VALUE
        val target = positionMs.coerceIn(0L, upper)
        p.seekTo(target)
        _uiState.update { it.copy(positionMs = target) }
    }

    fun setSpeed(speed: Float) {
        player?.setPlaybackSpeed(speed)
        preferences.playbackSpeed = speed
        _uiState.update { it.copy(playbackSpeed = speed) }
    }

    fun selectEpisode(index: Int) {
        playEpisode(_uiState.value.sourceIndex, index)
    }

    fun selectSource(index: Int) {
        val vod = detail ?: return
        val source = vod.playSources.getOrNull(index) ?: return
        if (source.episodes.isEmpty()) return
        val episode = _uiState.value.episodeIndex.coerceIn(0, source.episodes.size - 1)
        playEpisode(index, episode)
    }

    // ---------------------------------------------------------------- loading

    private fun loadPlayer() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            repository.getDetail(vodId)
                .onSuccess { vodDetail ->
                    detail = vodDetail
                    history = repository.getWatchHistory(vodDetail.id.ifBlank { vodId })

                    sourceIndex = sourceIndex.coerceIn(
                        0,
                        (vodDetail.playSources.size - 1).coerceAtLeast(0)
                    )
                    val episodes = vodDetail.playSources.getOrNull(sourceIndex)
                        ?.episodes ?: emptyList()
                    episodeIndex = episodeIndex.coerceIn(
                        0,
                        (episodes.size - 1).coerceAtLeast(0)
                    )

                    if (episodes.isEmpty()) {
                        _uiState.update {
                            it.copy(isLoading = false, error = "未找到播放源")
                        }
                        return@onSuccess
                    }

                    _uiState.update {
                        it.copy(
                            sources = vodDetail.playSources,
                            episodes = episodes,
                            vodTitle = vodDetail.title,
                            sourceIndex = sourceIndex,
                            episodeIndex = episodeIndex
                        )
                    }

                    // 只有历史记录与当前要播放的源/集匹配时才续播
                    val resume = history
                        ?.takeIf {
                            it.sourceIndex == sourceIndex && it.episodeIndex == episodeIndex
                        }
                        ?.position ?: 0L

                    resolveAndPlay(resumePosition = resume)
                    loadDanmaku(vodDetail.id.ifBlank { vodId })
                }
                .onFailure { e ->
                    if (e is CancellationException) return@onFailure
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message ?: "加载失败")
                    }
                }
        }
    }

    private fun resolveAndPlay(resumePosition: Long = 0) {
        playJob?.cancel()
        // Freeze the timeline at 0 until the new media is actually attached.
        mediaKey = null
        playJob = viewModelScope.launch {
            val vod = detail ?: return@launch
            val source = vod.playSources.getOrNull(sourceIndex) ?: return@launch
            val episode = source.episodes.getOrNull(episodeIndex) ?: return@launch

            _uiState.update {
                it.copy(
                    isLoading = true,
                    playUrl = null,
                    currentSource = source,
                    currentEpisode = episode,
                    episodes = source.episodes,
                    sourceIndex = sourceIndex,
                    episodeIndex = episodeIndex,
                    playbackKey = "${vod.id.ifBlank { vodId }}:$sourceIndex:$episodeIndex",
                    title = "${vod.title} - ${episode.name}",
                    resumePosition = resumePosition,
                    hasNext = episodeIndex + 1 < source.episodes.size,
                    // Reset the visible timeline immediately, before ExoPlayer swaps media,
                    // so the old episode's 43:45 / 00:00 cannot flash on the new episode.
                    positionMs = 0,
                    durationMs = 0,
                    bufferedMs = 0,
                    error = null
                )
            }

            repository.resolvePlayUrl(episode.playUrl)
                .onSuccess { resolved ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            playUrl = resolved.url,
                            playType = resolved.type
                        )
                    }
                }
                .onFailure { e ->
                    if (e is CancellationException) return@onFailure
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "解析播放地址失败: ${e.message}"
                        )
                    }
                }
        }
    }

    fun playEpisode(sourceIdx: Int, episodeIdx: Int) {
        val vod = detail ?: return
        val source = vod.playSources.getOrNull(sourceIdx) ?: return
        if (source.episodes.getOrNull(episodeIdx) == null) return
        sourceIndex = sourceIdx
        episodeIndex = episodeIdx
        resolveAndPlay(resumePosition = 0)
    }

    fun playNextEpisode() {
        val source = detail?.playSources?.getOrNull(sourceIndex) ?: return
        val next = episodeIndex + 1
        if (next < source.episodes.size) {
            playEpisode(sourceIndex, next)
        }
    }

    fun retry() {
        resolveAndPlay(
            resumePosition = _uiState.value.resumePosition
        )
    }

    /**
     * ExoPlayer 层的错误（如 CDN 403/断流），转成可重试的错误页。
     */
    fun reportPlaybackError(message: String, position: Long = 0) {
        _uiState.update {
            it.copy(
                isLoading = false,
                playUrl = null,
                resumePosition = position.coerceAtLeast(0),
                error = message
            )
        }
    }

    private fun loadDanmaku(vid: String) {
        if (vid.isBlank()) return
        viewModelScope.launch {
            repository.getDanmaku(vid)
                .onSuccess { danmaku ->
                    _uiState.update { it.copy(danmakuList = danmaku) }
                }
        }
    }

    fun toggleDanmaku() {
        val newValue = !_uiState.value.danmakuEnabled
        _uiState.update { it.copy(danmakuEnabled = newValue) }
        repository.setDanmakuEnabled(newValue)
    }

    /**
     * 由播放器在暂停/退出时上报，避免旧实现“每次解析成功就写 0 进度”。
     */
    fun saveProgress(position: Long, duration: Long) {
        val history = buildHistory(position, duration) ?: return
        viewModelScope.launch { repository.saveWatchHistory(history) }
    }

    /**
     * 播放器 onDispose 专用：走应用级作用域，避免 ViewModel 清理时写入被取消。
     */
    fun saveProgressOnExit(position: Long, duration: Long) {
        val history = buildHistory(position, duration) ?: return
        repository.saveWatchHistoryNow(history)
    }

    private fun buildHistory(position: Long, duration: Long): WatchHistory? {
        val vod = detail ?: return null
        val episode = _uiState.value.currentEpisode ?: return null
        if (_uiState.value.playUrl == null) return null
        return WatchHistory(
            vodId = vod.id,
            title = vod.title,
            coverUrl = vod.coverUrl,
            episodeIndex = episodeIndex,
            sourceIndex = sourceIndex,
            position = position.coerceAtLeast(0),
            duration = duration.coerceAtLeast(0)
        )
    }
}
