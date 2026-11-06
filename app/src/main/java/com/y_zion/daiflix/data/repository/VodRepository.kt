package com.y_zion.daiflix.data.repository

import com.y_zion.daiflix.data.api.DanmakuApi
import com.y_zion.daiflix.data.api.ResolveApi
import com.y_zion.daiflix.data.api.VidHubApi
import com.y_zion.daiflix.data.local.AppPreferences
import com.y_zion.daiflix.data.local.VodDao
import com.y_zion.daiflix.data.model.*
import com.y_zion.daiflix.data.parser.VodHtmlParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VodRepository @Inject constructor(
    private val vidHubApi: VidHubApi,
    private val resolveApi: ResolveApi,
    private val danmakuApi: DanmakuApi,
    private val parser: VodHtmlParser,
    private val dao: VodDao,
    private val preferences: AppPreferences
) {

    // ---- 网络请求 ----

    suspend fun getHomePage(): Result<com.y_zion.daiflix.data.parser.HomePageData> = runCatching {
        val html = vidHubApi.getHomePage()
        parser.parseHomePage(html)
    }

    suspend fun getCategory(category: Category, page: Int): Result<com.y_zion.daiflix.data.parser.CategoryData> = runCatching {
        val html = vidHubApi.getCategoryPage(
            categoryId = VidHubApi.getCategoryPath(category),
            page = page
        )
        parser.parseCategoryPage(html, category)
    }

    suspend fun search(keyword: String): Result<List<VodItem>> = runCatching {
        val html = vidHubApi.search(keyword = keyword)
        parser.parseSearchPage(html)
    }

    suspend fun getDetail(id: String): Result<VodDetail> = runCatching {
        val html = vidHubApi.getDetail(id = id)
        parser.parseDetailPage(html) ?: throw Exception("无法解析详情页")
    }

    suspend fun resolvePlayUrl(url: String): Result<ResolvedPlayUrl> = runCatching {
        // 从 URL 中提取 id, sourceId, episodeId
        // URL 格式: /vodplay/269371-1-1.html
        val pathRegex = """vodplay/(\d+)-(\d+)-(\d+)""".toRegex()
        val match = pathRegex.find(url) ?: throw Exception("无效的播放页 URL: $url")

        val id = match.groupValues[1]
        val sourceId = match.groupValues[2].toIntOrNull() ?: 1
        val episodeId = match.groupValues[3].toIntOrNull() ?: 1

        // 第一步：获取播放页 HTML，提取 iframe URL
        val playHtml = vidHubApi.getPlayPage(id = id, sourceId = sourceId, episodeId = episodeId)
        val iframeUrl = parser.parsePlayPageForIframe(playHtml)
            ?: throw Exception("未找到播放器 iframe")

        // 拼接完整 iframe URL
        val fullIframeUrl = if (iframeUrl.startsWith("http")) {
            iframeUrl
        } else {
            "https://vidhub3.top$iframeUrl"
        }

        // 第二步：获取 iframe 页面 HTML，提取实际视频 URL
        val playerHtml = vidHubApi.getPlayerPage(url = fullIframeUrl)
        parser.parsePlayerPage(playerHtml) ?: throw Exception("无法从播放器页面提取视频地址")
    }

    suspend fun getDanmaku(vid: String): Result<List<Danmaku>> = runCatching {
        val json = danmakuApi.getDanmaku(vid = vid)
        parser.parseDanmakuResponse(json)
    }

    // ---- 本地数据 ----

    fun getWatchHistory(): Flow<List<WatchHistory>> =
        dao.getWatchHistory().map { entities ->
            entities.map { it.toModel() }
        }

    suspend fun saveWatchHistory(history: WatchHistory) {
        dao.insertWatchHistory(history.toEntity())
    }

    suspend fun deleteWatchHistory(vodId: String) {
        dao.deleteWatchHistory(vodId)
    }

    suspend fun clearWatchHistory() {
        dao.clearWatchHistory()
    }

    fun getSearchHistory(): Flow<List<SearchHistory>> =
        dao.getSearchHistory().map { entities ->
            entities.map { it.toModel() }
        }

    suspend fun saveSearchHistory(keyword: String) {
        val existing = dao.getSearchByKeyword(keyword)
        if (existing != null) {
            dao.insertSearchHistory(existing.copy(timestamp = System.currentTimeMillis()))
        } else {
            dao.insertSearchHistory(SearchHistoryEntity(keyword = keyword))
        }
    }

    suspend fun clearSearchHistory() {
        dao.clearSearchHistory()
    }

    fun getFavorites(): Flow<List<VodItem>> =
        dao.getFavorites().map { entities ->
            entities.map {
                VodItem(
                    id = it.vodId,
                    title = it.title,
                    coverUrl = it.coverUrl,
                    category = Category.fromValue(it.category)
                )
            }
        }

    suspend fun isFavorite(vodId: String): Boolean = dao.isFavorite(vodId)

    suspend fun toggleFavorite(vod: VodDetail) {
        if (dao.isFavorite(vod.id)) {
            dao.deleteFavorite(vod.id)
        } else {
            dao.insertFavorite(
                FavoriteEntity(
                    vodId = vod.id,
                    title = vod.title,
                    coverUrl = vod.coverUrl,
                    category = vod.category.value
                )
            )
        }
    }

    // ---- 设置 ----

    fun getBaseUrl(): String = preferences.baseUrl

    fun setBaseUrl(url: String) {
        preferences.baseUrl = url
    }

    fun isDanmakuEnabled(): Boolean = preferences.danmakuEnabled

    fun setDanmakuEnabled(enabled: Boolean) {
        preferences.danmakuEnabled = enabled
    }
}

// ---- 扩展函数 ----

private fun WatchHistoryEntity.toModel() = WatchHistory(
    vodId = vodId,
    title = title,
    coverUrl = coverUrl,
    episodeIndex = episodeIndex,
    position = position,
    duration = duration,
    timestamp = timestamp
)

private fun WatchHistory.toEntity() = WatchHistoryEntity(
    vodId = vodId,
    title = title,
    coverUrl = coverUrl,
    episodeIndex = episodeIndex,
    position = position,
    duration = duration,
    timestamp = timestamp
)

private fun SearchHistoryEntity.toModel() = SearchHistory(
    id = id,
    keyword = keyword,
    timestamp = timestamp
)
