package com.y_zion.daiflix.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.y_zion.daiflix.data.local.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class SettingsUiState(
    val baseUrl: String = "",
    val baseUrlError: String? = null,
    val danmakuEnabled: Boolean = true,
    val danmakuOpacity: Float = 0.8f,
    val danmakuSize: Float = 1.0f,
    val danmakuSpeed: Int = 1,
    val playbackSpeed: Float = 1.0f,
    val autoPlayNext: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(loadSettings())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private fun loadSettings(): SettingsUiState {
        return SettingsUiState(
            baseUrl = preferences.baseUrl,
            danmakuEnabled = preferences.danmakuEnabled,
            danmakuOpacity = preferences.danmakuOpacity,
            danmakuSize = preferences.danmakuSize,
            danmakuSpeed = preferences.danmakuSpeed,
            playbackSpeed = preferences.playbackSpeed,
            autoPlayNext = preferences.autoPlayNext
        )
    }

    fun saveBaseUrl(url: String) {
        val normalized = url.trim().trimEnd('/')
        val error = when {
            normalized.isBlank() -> "地址不能为空"
            !normalized.startsWith("http://") && !normalized.startsWith("https://") ->
                "地址必须以 http:// 或 https:// 开头"
            normalized.contains(' ') -> "地址不能包含空格"
            runCatching { java.net.URI(normalized).host }.getOrNull().isNullOrBlank() ->
                "地址格式不正确"
            else -> null
        }
        if (error != null) {
            _uiState.value = _uiState.value.copy(baseUrlError = error)
            return
        }
        preferences.baseUrl = normalized
        _uiState.value = _uiState.value.copy(baseUrl = normalized, baseUrlError = null)
    }

    fun resetBaseUrl() {
        preferences.baseUrl = AppPreferences.DEFAULT_BASE_URL
        _uiState.value = _uiState.value.copy(
            baseUrl = AppPreferences.DEFAULT_BASE_URL,
            baseUrlError = null
        )
    }

    fun setDanmakuEnabled(enabled: Boolean) {
        preferences.danmakuEnabled = enabled
        _uiState.value = _uiState.value.copy(danmakuEnabled = enabled)
    }

    fun setDanmakuOpacity(opacity: Float) {
        preferences.danmakuOpacity = opacity
        _uiState.value = _uiState.value.copy(danmakuOpacity = opacity)
    }

    fun setDanmakuSize(size: Float) {
        preferences.danmakuSize = size
        _uiState.value = _uiState.value.copy(danmakuSize = size)
    }

    fun setDanmakuSpeed(speed: Int) {
        preferences.danmakuSpeed = speed
        _uiState.value = _uiState.value.copy(danmakuSpeed = speed)
    }

    fun setPlaybackSpeed(speed: Float) {
        preferences.playbackSpeed = speed
        _uiState.value = _uiState.value.copy(playbackSpeed = speed)
    }

    fun setAutoPlayNext(auto: Boolean) {
        preferences.autoPlayNext = auto
        _uiState.value = _uiState.value.copy(autoPlayNext = auto)
    }
}
