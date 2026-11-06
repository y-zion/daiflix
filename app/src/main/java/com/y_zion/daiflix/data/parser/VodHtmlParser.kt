package com.y_zion.daiflix.data.parser

import com.y_zion.daiflix.data.model.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 解析 VidHub 网页 HTML，提取视频数据
 * 基于实际页面结构：.module-item, .video-info-item, .sort-list 等
 */
@Singleton
class VodHtmlParser @Inject constructor() {

    /**
     * 解析首页 HTML，提取推荐和各分类内容
     */
    fun parseHomePage(html: String): HomePageData {
        val doc = Jsoup.parse(html)

        // 提取 Banner（轮播推荐）
        val banners = parseBanners(doc)

        // 提取各分类行（正在热播、电影、电视剧等）
        val rows = mutableListOf<VodRow>()

        // 查找所有 .module-list.module-lines-list 容器
        doc.select(".module-list.module-lines-list").forEach { rowEl ->
            // 找到对应的标题（在前面的 .module-heading 中）
            val heading = rowEl.previousElementSibling()
            val title = heading?.selectFirst("h2")?.text()?.trim() ?: return@forEach

            // 提取该行的所有视频项
            val items = rowEl.select(".module-item").mapNotNull { el ->
                parseVodItemFromModule(el, Category.HOME)
            }.filter { it.coverUrl.isNotBlank() }

            if (items.isNotEmpty()) {
                rows.add(VodRow(title = title, items = items))
            }
        }

        return HomePageData(banners = banners, rows = rows)
    }

    /**
     * 解析分类列表页
     */
    fun parseCategoryPage(html: String, category: Category): CategoryData {
        val doc = Jsoup.parse(html)
        val items = mutableListOf<VodItem>()

        // 查找所有 .module-item
        doc.select(".module-item").forEach { el ->
            val item = parseVodItemFromModule(el, category)
            if (item != null && item.coverUrl.isNotBlank()) {
                items.add(item)
            }
        }

        // 解析分页
        val hasMore = doc.select(".page-next, a:contains(下一页), a:contains(>>), .mac_page a:contains(>)").isNotEmpty()

        return CategoryData(items = items, hasMore = hasMore)
    }

    /**
     * 解析搜索结果页
     */
    fun parseSearchPage(html: String): List<VodItem> {
        val doc = Jsoup.parse(html)
        val items = mutableListOf<VodItem>()

        doc.select(".module-item").forEach { el ->
            val item = parseVodItemFromModule(el, Category.HOME)
            if (item != null && item.coverUrl.isNotBlank()) {
                items.add(item)
            }
        }

        return items
    }

    /**
     * 解析视频详情页
     */
    fun parseDetailPage(html: String): VodDetail? {
        val doc = Jsoup.parse(html)

        // 提取标题
        val title = doc.selectFirst("h1.page-title")?.text()?.trim() ?: return null

        // 提取封面图
        val coverUrl = doc.selectFirst(".video-info-item img[data-src], .video-info-item img[src]")
            ?.let { img ->
                img.attr("data-src").ifBlank { img.attr("src") }
            }?.trim() ?: ""

        // 提取背景图（如果有）
        val backdropUrl = doc.selectFirst(".video-info-bg, [class*=backdrop], [class*=banner]")
            ?.attr("style")?.let { extractUrlFromStyle(it) } ?: ""

        // 提取元数据
        val videoInfoItems = doc.select(".video-info-items")
        val year = extractInfoValue(videoInfoItems, "上映", "年份", "年代")
        val region = extractInfoValue(videoInfoItems, "地区", "国家")
        val rating = extractInfoValue(videoInfoItems, "评分")
        val director = extractInfoValue(videoInfoItems, "导演")
        val actors = extractInfoValue(videoInfoItems, "主演", "演员")
        val description = doc.selectFirst(".video-info-content.vod_content span")
            ?.text()?.trim() ?: ""

        // 提取分类
        val categoryText = extractInfoValue(videoInfoItems, "类型", "分类")
        val category = inferCategory(categoryText, title)

        // 提取播放源和集数
        val playSources = parsePlaySources(doc)

        // 提取相关推荐
        val relatedItems = mutableListOf<VodItem>()
        doc.select(".module-item").forEach { el ->
            val item = parseVodItemFromModule(el, Category.HOME)
            if (item != null && item.coverUrl.isNotBlank() && item.id != extractIdFromUrl(doc.location())) {
                relatedItems.add(item)
            }
        }

        return VodDetail(
            id = extractIdFromUrl(doc.location()) ?: "",
            title = title,
            coverUrl = coverUrl,
            backdropUrl = backdropUrl,
            category = category,
            year = year,
            region = region,
            rating = rating,
            director = director,
            actors = actors,
            description = description,
            playSources = playSources,
            relatedItems = relatedItems.take(20)
        )
    }

    /**
     * 解析播放页，提取播放器 iframe URL
     * 返回 iframe 的完整 URL，用于第二步抓取
     */
    fun parsePlayPageForIframe(html: String): String? {
        val doc = Jsoup.parse(html)

        // 提取播放器 iframe src
        doc.select("iframe").forEach { iframe ->
            val src = iframe.attr("src")
            if (src.isNotBlank() && (src.contains("player") || src.contains("play"))) {
                // 如果是相对路径，返回原样（由调用方拼接 base URL）
                return src
            }
        }

        return null
    }

    /**
     * 解析播放器 iframe 页面，提取实际视频 URL
     * 从 var config = {"url": "...", "type": "..."} 中提取
     */
    fun parsePlayerPage(html: String): ResolvedPlayUrl? {
        val doc = Jsoup.parse(html)

        // 从 script 中提取 config 变量
        val scripts = doc.select("script")
        for (script in scripts) {
            val data = script.data()
            // 匹配 var config = {...}
            val configMatch = """var\s+config\s*=\s*(\{[^;]+\})""".toRegex().find(data)
            if (configMatch != null) {
                val configJson = configMatch.groupValues[1]
                // 从 config JSON 中提取 url 和 type
                val urlRegex = """"url"\s*:\s*"([^"]+)"""".toRegex()
                val typeRegex = """"type"\s*:\s*"?(\w+)"?""".toRegex()

                val url = urlRegex.find(configJson)?.groupValues?.get(1)
                    ?.replace("\\/", "/") ?: continue
                val type = typeRegex.find(configJson)?.groupValues?.get(1) ?: ""

                val playType = when {
                    url.contains(".m3u8") || type == "27" || type.contains("hls", ignoreCase = true) -> PlayType.HLS
                    url.contains(".mp4") || type.contains("mp4", ignoreCase = true) -> PlayType.MP4
                    url.contains(".mpd") || type.contains("dash", ignoreCase = true) -> PlayType.DASH
                    else -> PlayType.HLS
                }

                return ResolvedPlayUrl(url = url, type = playType)
            }

            // 备用：直接查找 url 字段
            val directUrlPattern = """"url"\s*:\s*"([^"]+)"""".toRegex()
            directUrlPattern.find(data)?.let { match ->
                val url = match.groupValues[1].replace("\\/", "/")
                if (url.startsWith("http") && (url.contains(".m3u8") || url.contains(".mp4"))) {
                    val playType = if (url.contains(".m3u8")) PlayType.HLS else PlayType.MP4
                    return ResolvedPlayUrl(url = url, type = playType)
                }
            }
        }

        return null
    }

    /**
     * 解析解析 API 的 JSON 响应
     */
    fun parseResolveResponse(json: String): ResolvedPlayUrl? {
        return try {
            // 简单 JSON 解析（避免引入额外依赖）
            val urlRegex = """"url"\s*:\s*"([^"]+)"""".toRegex()
            val typeRegex = """"type"\s*:\s*"?(\w+)"?""".toRegex()

            val url = urlRegex.find(json)?.groupValues?.get(1) ?: return null
            val type = typeRegex.find(json)?.groupValues?.get(1) ?: "hls"

            val playType = when {
                url.contains(".m3u8") || type.contains("hls", ignoreCase = true) -> PlayType.HLS
                url.contains(".mp4") || type.contains("mp4", ignoreCase = true) -> PlayType.MP4
                url.contains(".mpd") || type.contains("dash", ignoreCase = true) -> PlayType.DASH
                else -> PlayType.HLS
            }

            ResolvedPlayUrl(
                url = url.replace("\\/", "/"),
                type = playType
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 解析弹幕 JSON
     */
    fun parseDanmakuResponse(json: String): List<Danmaku> {
        val danmakuList = mutableListOf<Danmaku>()
        try {
            // 弹幕格式: [[time, type, color, text], ...] 或 {data: [...]}
            val arrayRegex = """\[\s*(\d+\.?\d*)\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*"([^"]*)"\s*]""".toRegex()
            arrayRegex.findAll(json).forEach { match ->
                danmakuList.add(
                    Danmaku(
                        time = match.groupValues[1].toFloat(),
                        type = match.groupValues[2].toInt(),
                        color = match.groupValues[3].toIntOrNull() ?: 0xFFFFFF,
                        text = match.groupValues[4]
                    )
                )
            }
        } catch (_: Exception) {}
        return danmakuList
    }

    // ---- 私有辅助方法 ----

    private fun parseBanners(doc: Document): List<VodItem> {
        val banners = mutableListOf<VodItem>()
        doc.select(".swiper-slide a.dymrslide, .swiper-slide a[href*=voddetail]").forEach { el ->
            val href = el.attr("href")
            val id = extractIdFromUrl(href) ?: return@forEach
            val title = el.selectFirst(".name, p")?.text()?.trim()
                ?: el.attr("title")?.trim()
                ?: return@forEach

            // 封面图从 style 背景或 img 中提取
            val coverUrl = el.attr("style").let { extractUrlFromStyle(it) }
                .ifBlank {
                    el.selectFirst("img")?.let { img ->
                        img.attr("data-src").ifBlank { img.attr("src") }
                    } ?: ""
                }

            if (title.isNotBlank() && coverUrl.isNotBlank()) {
                banners.add(
                    VodItem(
                        id = id,
                        title = title,
                        coverUrl = coverUrl,
                        category = Category.HOME
                    )
                )
            }
        }
        return banners
    }

    private fun parseVodItemFromModule(el: org.jsoup.nodes.Element, defaultCategory: Category): VodItem? {
        // 从 .module-item 中提取视频信息
        val link = el.selectFirst("a[href*=voddetail]") ?: return null
        val href = link.attr("href")
        val id = extractIdFromUrl(href) ?: return null

        val title = el.selectFirst(".video-name a, .module-item-style.video-name a")?.text()?.trim()
            ?: link.attr("title")?.trim()
            ?: return null

        val coverUrl = el.selectFirst("img")?.let { img ->
            img.attr("data-src").ifBlank { img.attr("data-original") }.ifBlank { img.attr("src") }
        }?.trim() ?: ""

        val year = el.selectFirst(".module-item-caption span:first-child")?.text()?.trim() ?: ""
        val categoryText = el.selectFirst(".module-item-caption .video-class")?.text()?.trim() ?: ""
        val region = el.selectFirst(".module-item-caption span:last-child")?.text()?.trim() ?: ""

        val description = el.selectFirst(".video-text, .module-item-style.video-text")?.text()?.trim() ?: ""
        val episodeCount = el.selectFirst(".module-item-caption .video-class + span")?.text()?.trim() ?: ""

        val category = inferCategory(categoryText, title)

        return VodItem(
            id = id,
            title = title,
            coverUrl = coverUrl,
            category = category,
            year = year,
            rating = "",
            description = description,
            episodeCount = episodeCount,
            updateStatus = ""
        )
    }

    private fun parsePlaySources(doc: Document): List<PlaySource> {
        val sources = mutableListOf<PlaySource>()

        // 查找播放源标签 (.module-tab-item)
        val sourceTabs = doc.select(".module-tab-item.tab-item")
        val sourceLists = doc.select(".module-list.module-player-list")

        if (sourceTabs.isNotEmpty()) {
            sourceTabs.forEachIndexed { index, tab ->
                val name = tab.text().trim().ifBlank { "播放源${index + 1}" }
                val episodes = mutableListOf<Episode>()

                // 查找对应的集数列表
                val listContainer = sourceLists.getOrNull(index)
                listContainer?.select(".sort-item a, .module-blocklist a")?.forEachIndexed { epIndex, epLink ->
                    val epName = epLink.text().trim().ifBlank { "第${epIndex + 1}集" }
                    val epUrl = epLink.attr("href")
                    val epId = extractEpisodeId(epUrl)

                    episodes.add(
                        Episode(
                            index = epId?.third ?: (epIndex + 1),
                            name = epName,
                            playUrl = epUrl
                        )
                    )
                }

                if (episodes.isNotEmpty()) {
                    sources.add(PlaySource(name = name, episodes = episodes))
                }
            }
        } else {
            // 没有多源，尝试直接提取播放列表
            val episodes = mutableListOf<Episode>()
            doc.select(".module-list a[href*=vodplay], .sort-list a[href*=vodplay]").forEachIndexed { index, link ->
                val name = link.text().trim().ifBlank { "第${index + 1}集" }
                val url = link.attr("href")
                val epId = extractEpisodeId(url)

                episodes.add(
                    Episode(
                        index = epId?.third ?: (index + 1),
                        name = name,
                        playUrl = url
                    )
                )
            }
            if (episodes.isNotEmpty()) {
                sources.add(PlaySource(name = "默认", episodes = episodes))
            }
        }

        return sources
    }

    private fun extractInfoValue(infoItems: org.jsoup.select.Elements, vararg keys: String): String {
        infoItems.forEach { container ->
            val title = container.selectFirst(".video-info-itemtitle")?.text() ?: ""
            for (key in keys) {
                if (title.contains(key)) {
                    return container.selectFirst(".video-info-item")?.text()?.trim()
                        ?.substringAfter(":")?.substringAfter("：")?.trim() ?: ""
                }
            }
        }
        return ""
    }

    private fun extractIdFromUrl(url: String): String? {
        val regex = """(?:voddetail|vodplay|vodsearch)/(\d+)""".toRegex()
        return regex.find(url)?.groupValues?.get(1)
    }

    private fun extractEpisodeId(url: String): Triple<String, Int, Int>? {
        // /vodplay/269371-1-1.html -> (269371, 1, 1)
        val regex = """vodplay/(\d+)-(\d+)-(\d+).html""".toRegex()
        val match = regex.find(url) ?: return null
        return Triple(
            match.groupValues[1],
            match.groupValues[2].toIntOrNull() ?: 1,
            match.groupValues[3].toIntOrNull() ?: 1
        )
    }

    private fun extractUrlFromStyle(style: String): String {
        val regex = """url\(['"]?([^'")]+)['"]?\)""".toRegex()
        return regex.find(style)?.groupValues?.get(1) ?: ""
    }

    private fun inferCategory(categoryText: String, title: String): Category {
        val text = "$categoryText $title"
        return when {
            text.contains("综艺") || text.contains("variety") -> Category.VARIETY
            text.contains("动漫") || text.contains("动画") || text.contains("anime") -> Category.ANIME
            text.contains("纪录") || text.contains("documentary") -> Category.DOCUMENTARY
            text.contains("电视剧") || text.contains("连续剧") || text.contains("tv", ignoreCase = true) -> Category.TV_SERIES
            text.contains("电影") || text.contains("movie", ignoreCase = true) -> Category.MOVIE
            else -> Category.MOVIE
        }
    }
}

/**
 * 首页数据
 */
data class HomePageData(
    val banners: List<VodItem> = emptyList(),
    val rows: List<VodRow> = emptyList()
)

/**
 * 首页行数据
 */
data class VodRow(
    val title: String,
    val items: List<VodItem>
)

/**
 * 分类数据
 */
data class CategoryData(
    val items: List<VodItem> = emptyList(),
    val hasMore: Boolean = false
)
