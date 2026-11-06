package com.y_zion.daiflix.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.y_zion.daiflix.data.model.*
import com.y_zion.daiflix.data.repository.VodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
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
    val title: String = "",
    val error: String? = null
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val repository: VodRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val vodId: String = savedStateHandle["vodId"] ?: ""
    private val sourceIndex: Int = savedStateHandle["sourceIndex"] ?: 0
    private val episodeIndex: Int = savedStateHandle["episodeIndex"] ?: 0

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var detail: VodDetail? = null

    init {
        _uiState.update { it.copy(danmakuEnabled = repository.isDanmakuEnabled()) }
        loadPlayer()
    }

    private fun loadPlayer() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // 获取详情
            repository.getDetail(vodId)
                .onSuccess { vodDetail ->
                    detail = vodDetail
                    val source = vodDetail.playSources.getOrNull(sourceIndex)
                    val episode = source?.episodes?.getOrNull(episodeIndex)

                    if (episode == null) {
                        _uiState.update {
                            it.copy(isLoading = false, error = "未找到播放源")
                        }
                        return@onSuccess
                    }

                    _uiState.update {
                        it.copy(
                            currentSource = source,
                            currentEpisode = episode,
                            title = "${vodDetail.title} - ${episode.name}"
                        )
                    }

                    // 解析播放地址
                    resolveAndPlay(episode.playUrl)

                    // 加载弹幕
                    if (_uiState.value.danmakuEnabled) {
                        loadDanmaku(vodId)
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message ?: "加载失败")
                    }
                }
        }
    }

    private suspend fun resolveAndPlay(pageUrl: String) {
        repository.resolvePlayUrl(pageUrl)
            .onSuccess { resolved ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        playUrl = resolved.url,
                        playType = resolved.type
                    )
                }
                // 保存观看历史
                saveHistory(position = 0, duration = 0)
            }
            .onFailure { e ->
                _uiState.update {
                    it.copy(isLoading = false, error = "解析播放地址失败: ${e.message}")
                }
            }
    }

    fun playEpisode(sourceIdx: Int, episodeIdx: Int) {
        viewModelScope.launch {
            val vod = detail ?: return@launch
            val source = vod.playSources.getOrNull(sourceIdx) ?: return@launch
            val episode = source.episodes.getOrNull(episodeIdx) ?: return@launch

            _uiState.update {
                it.copy(
                    isLoading = true,
                    currentSource = source,
                    currentEpisode = episode,
                    title = "${vod.title} - ${episode.name}",
                    error = null
                )
            }

            resolveAndPlay(episode.playUrl)

            if (_uiState.value.danmakuEnabled) {
                loadDanmaku(vodId)
            }
        }
    }

    fun playNextEpisode() {
        val state = _uiState.value
        val source = state.currentSource ?: return
        val currentEp = state.currentEpisode ?: return
        val nextIdx = currentEp.index  // index is 1-based, list index is 0-based
        if (nextIdx < source.episodes.size) {
            playEpisode(sourceIndex, nextIdx)
        }
    }

    private fun loadDanmaku(vid: String) {
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

    fun saveHistory(position: Long, duration: Long) {
        viewModelScope.launch {
            val ep = _uiState.value.currentEpisode ?: return@launch
            val vod = detail ?: return@launch
            repository.saveWatchHistory(
                WatchHistory(
                    vodId = vod.id,
                    title = vod.title,
                    coverUrl = vod.coverUrl,
                    episodeIndex = ep.index,
                    position = position,
                    duration = duration
                )
            )
        }
    }
}
