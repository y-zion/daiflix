package com.yzion.daiflix.tv.data.model

/**
 * 视频分类
 */
enum class Category(val value: String, val displayName: String) {
    HOME("home", "首页"),
    MOVIE("1", "电影"),
    TV_SERIES("2", "电视剧"),
    VARIETY("3", "综艺"),
    ANIME("4", "动漫"),
    DOCUMENTARY("5", "纪录片");

    companion object {
        fun fromValue(value: String): Category =
            entries.firstOrNull { it.value == value } ?: HOME
    }
}

/**
 * 视频列表项（精简模型，用于列表展示）
 */
data class VodItem(
    val id: String,
    val title: String,
    val coverUrl: String,
    val category: Category,
    val year: String = "",
    val rating: String = "",
    val description: String = "",
    val episodeCount: String = "",
    val updateStatus: String = ""
)

/**
 * 视频详情
 */
data class VodDetail(
    val id: String,
    val title: String,
    val coverUrl: String,
    val backdropUrl: String = "",
    val category: Category,
    val year: String = "",
    val region: String = "",
    val rating: String = "",
    val director: String = "",
    val actors: String = "",
    val description: String = "",
    val playSources: List<PlaySource> = emptyList(),
    val relatedItems: List<VodItem> = emptyList()
)

/**
 * 播放源（一个视频可能有多个播放源）
 */
data class PlaySource(
    val name: String,           // 如 "高清", "蓝光", "极速"
    val episodes: List<Episode> = emptyList()
)

/**
 * 单集
 */
data class Episode(
    val index: Int,             // 集号
    val name: String,           // 集名（如 "第1集", "全集"）
    val playUrl: String = "",   // 播放页面 URL（需要 resolve）
    val directUrl: String = ""  // 直接播放 URL（resolve 后获得）
)

/**
 * 解析后的播放地址
 */
data class ResolvedPlayUrl(
    val url: String,
    val type: PlayType = PlayType.HLS,
    val headers: Map<String, String> = emptyMap()
)

enum class PlayType {
    HLS,    // m3u8
    MP4,    // 直接 mp4
    DASH    // dash (mpd)
}

/**
 * 弹幕
 */
data class Danmaku(
    val text: String,
    val time: Float,       // 出现时间（秒）
    val color: Int = 0xFFFFFF,
    val type: Int = 0      // 0=滚动, 1=顶部, 2=底部
)

/**
 * 搜索历史
 */
data class SearchHistory(
    val id: Long = 0,
    val keyword: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 观看历史
 */
data class WatchHistory(
    val id: Long = 0,
    val vodId: String,
    val title: String,
    val coverUrl: String,
    val episodeIndex: Int,       // 0-based index inside the selected source
    val sourceIndex: Int = 0,    // 0-based index into VodDetail.playSources
    val position: Long = 0,     // 播放进度（毫秒）
    val duration: Long = 0,     // 总时长（毫秒）
    val timestamp: Long = System.currentTimeMillis()
) {
    val progress: Float
        get() = if (duration > 0) position.toFloat() / duration else 0f
}
