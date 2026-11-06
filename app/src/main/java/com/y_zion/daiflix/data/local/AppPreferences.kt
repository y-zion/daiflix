package com.y_zion.daiflix.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * 应用设置（轻量级，使用 SharedPreferences）
 */
class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("daiflix_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_DANMAKU_ENABLED = "danmaku_enabled"
        private const val KEY_DANMAKU_OPACITY = "danmaku_opacity"
        private const val KEY_DANMAKU_SIZE = "danmaku_size"
        private const val KEY_DANMAKU_SPEED = "danmaku_speed"
        private const val KEY_PLAYBACK_SPEED = "playback_speed"
        private const val KEY_AUTO_PLAY_NEXT = "auto_play_next"
        private const val KEY_LAST_CATEGORY = "last_category"

        private const val DEFAULT_BASE_URL = "https://vidhub3.top"
    }

    var baseUrl: String
        get() = prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
        set(value) = prefs.edit { putString(KEY_BASE_URL, value) }

    var danmakuEnabled: Boolean
        get() = prefs.getBoolean(KEY_DANMAKU_ENABLED, true)
        set(value) = prefs.edit { putBoolean(KEY_DANMAKU_ENABLED, value) }

    var danmakuOpacity: Float
        get() = prefs.getFloat(KEY_DANMAKU_OPACITY, 0.8f)
        set(value) = prefs.edit { putFloat(KEY_DANMAKU_OPACITY, value) }

    var danmakuSize: Float
        get() = prefs.getFloat(KEY_DANMAKU_SIZE, 1.0f)
        set(value) = prefs.edit { putFloat(KEY_DANMAKU_SIZE, value) }

    var danmakuSpeed: Int
        get() = prefs.getInt(KEY_DANMAKU_SPEED, 1)
        set(value) = prefs.edit { putInt(KEY_DANMAKU_SPEED, value) }

    var playbackSpeed: Float
        get() = prefs.getFloat(KEY_PLAYBACK_SPEED, 1.0f)
        set(value) = prefs.edit { putFloat(KEY_PLAYBACK_SPEED, value) }

    var autoPlayNext: Boolean
        get() = prefs.getBoolean(KEY_AUTO_PLAY_NEXT, true)
        set(value) = prefs.edit { putBoolean(KEY_AUTO_PLAY_NEXT, value) }

    var lastCategory: String
        get() = prefs.getString(KEY_LAST_CATEGORY, "movie") ?: "movie"
        set(value) = prefs.edit { putString(KEY_LAST_CATEGORY, value) }
}
