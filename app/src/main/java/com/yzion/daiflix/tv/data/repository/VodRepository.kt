package com.yzion.daiflix.tv.data.repository

import com.yzion.daiflix.tv.data.api.DanmakuApi
import com.yzion.daiflix.tv.data.api.ResolveApi
import com.yzion.daiflix.tv.data.api.VidHubApi
import com.yzion.daiflix.tv.data.local.AppPreferences
import com.yzion.daiflix.tv.data.local.VodDao
import com.yzion.daiflix.tv.data.model.*
import com.yzion.daiflix.tv.data.parser.VodHtmlParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.net.URLDecoder
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

    /**
     * 应用级写入作用域：播放器退出时 ViewModel 可能已被清理，
     * 用这个作用域保证最后一次进度一定落库。
     */
    private val persistScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val currentBaseUrl: String
        get() = preferences.baseUrl.trimEnd('/')

    @Volatile
    private var cachedDetail: VodDetail? = null

    suspend fun getHomePage(): Result<com.yzion.daiflix.tv.data.parser.HomePageData> = runCatching {
        val html = vidHubApi.getHomePage()
        parser.parseHomePage(html)
    }

    suspend fun getCategory(category: Category): Result<com.yzion.daiflix.tv.data.parser.CategoryData> = runCatching {
        val html = vidHubApi.getCategoryPage(
            categoryId = VidHubApi.getCategoryPath(category)
        )
        parser.parseCategoryPage(html, category)
    }

    /**
     * 网页搜索被“人机验证”拦截，改用站点的 suggest JSON 接口。
     */
    suspend fun search(keyword: String): Result<List<VodItem>> = runCatching {
        val json = vidHubApi.suggest(keyword = keyword)
        parser.parseSuggestResponse(json)
    }

    suspend fun getDetail(id: String): Result<VodDetail> {
        val cached = cachedDetail
        if (cached != null && cached.id == id) {
            return Result.success(cached)
        }
        return runCatching {
            val html = vidHubApi.getDetail(id = id)
            val pageUrl = "$currentBaseUrl/voddetail/$id.html"
            val detail = parser.parseDetailPage(html, pageUrl)
                ?: throw Exception("无法解析详情页")
            // 即使页面里的 canonical URL 缺失，也保证详情 ID 来自路由。
            detail.copy(id = detail.id.ifBlank { id }).also { cachedDetail = it }
        }
    }

    suspend fun resolvePlayUrl(url: String): Result<ResolvedPlayUrl> = runCatching {
        // 从 URL 中提取 id, sourceId, episodeId
        // URL 格式: /vodplay/269371-1-1.html
        val pathRegex = """vodplay/(\d+)-(\d+)-(\d+)""".toRegex()
        val match = pathRegex.find(url) ?: throw Exception("无效的播放页 URL: $url")

        val id = match.groupValues[1]
        val sourceId = match.groupValues[2].toIntOrNull() ?: 1
        val episodeId = match.groupValues[3].toIntOrNull() ?: 1

        // 第一步：获取播放页 HTML，提取播放器 iframe URL
        val playHtml = vidHubApi.getPlayPage(id = id, sourceId = sourceId, episodeId = episodeId)
        val iframeUrl = parser.parsePlayPageForIframe(playHtml)
            ?: throw Exception("未找到播放器 iframe")

        // 第二步：优先走 /player/api/resolve.php?u={encrypted}。
        // 旧逻辑去 iframe 页面里找 config.url，但站点现在把 url 留空，
        // 真实地址由 player 的 JS 调用 resolve 接口获得。
        val resolveToken = queryParameter(iframeUrl, "u")
        if (!resolveToken.isNullOrBlank()) {
            val json = resolveApi.resolve(encryptedUrl = resolveToken)
            parser.parseResolveResponse(json)
                ?: throw Exception("播放地址解析失败")
        } else {
            // 少数页面直接在 config 中给出 url，保留兜底路径。
            val fullIframeUrl = if (iframeUrl.startsWith("http")) {
                iframeUrl
            } else {
                "$currentBaseUrl$iframeUrl"
            }
            val playerHtml = vidHubApi.getPlayerPage(url = fullIframeUrl)
            parser.parsePlayerPage(playerHtml)
                ?: throw Exception("无法从播放器页面提取视频地址")
        }
    }

    suspend fun getDanmaku(vid: String): Result<List<Danmaku>> = runCatching {
        val json = danmakuApi.getDanmaku(id = vid)
        parser.parseDanmakuResponse(json)
    }

    // ---- 本地数据 ----

    fun getWatchHistory(): Flow<List<WatchHistory>> =
        dao.getWatchHistory().map { entities ->
            entities.map { it.toModel() }
        }

    suspend fun getWatchHistory(vodId: String): WatchHistory? =
        dao.getWatchHistoryById(vodId)?.toModel()

    suspend fun saveWatchHistory(history: WatchHistory) {
        // A detail page that failed to expose its cover must not wipe the artwork
        // we already resolved earlier (the continue-watching grid would go blank).
        val cover = history.coverUrl.ifBlank {
            dao.getWatchHistoryById(history.vodId)?.coverUrl.orEmpty()
        }
        dao.insertWatchHistory(history.copy(coverUrl = cover).toEntity())
    }

    fun saveWatchHistoryNow(history: WatchHistory) {
        persistScope.launch { saveWatchHistory(history) }
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

    fun getPlaybackSpeed(): Float = preferences.playbackSpeed

    fun isAutoPlayNext(): Boolean = preferences.autoPlayNext

    fun getDanmakuOpacity(): Float = preferences.danmakuOpacity

    fun getDanmakuSize(): Float = preferences.danmakuSize

    private fun queryParameter(url: String, name: String): String? {
        val query = url.substringAfter('?', missingDelimiterValue = "")
        if (query.isBlank()) return null
        return query.split('&').firstOrNull { part ->
            part.substringBefore('=') == name
        }?.substringAfter('=', missingDelimiterValue = "")?.let {
            runCatching { URLDecoder.decode(it, "UTF-8") }.getOrNull()
        }
    }
}

// ---- 扩展函数 ----

private fun WatchHistoryEntity.toModel() = WatchHistory(
    // P0 fix: the model id defaulted to 0 for every row, which made
    // LazyVerticalGrid(itemsIndexed(..., key = { it.id })) crash with
    // "Key 0 was already used". Derive a stable unique id from the vodId.
    id = vodId.toLongOrNull() ?: vodId.hashCode().toLong(),
    vodId = vodId,
    title = title,
    coverUrl = coverUrl,
    episodeIndex = episodeIndex,
    sourceIndex = sourceIndex,
    position = position,
    duration = duration,
    timestamp = timestamp
)

private fun WatchHistory.toEntity() = WatchHistoryEntity(
    vodId = vodId,
    title = title,
    coverUrl = coverUrl,
    episodeIndex = episodeIndex,
    sourceIndex = sourceIndex,
    position = position,
    duration = duration,
    timestamp = timestamp
)

private fun SearchHistoryEntity.toModel() = SearchHistory(
    id = id,
    keyword = keyword,
    timestamp = timestamp
)
