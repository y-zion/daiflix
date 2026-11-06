package com.y_zion.daiflix.data.parser

import com.y_zion.daiflix.data.model.PlayType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VodHtmlParserTest {

    private val parser = VodHtmlParser()

    @Test
    fun `detail keeps id from page url and dedupes duplicated episode anchors`() {
        val html = """
            <html><body>
              <h1 class="page-title">测试影片</h1>
              <div class="video-info-items">
                <div class="video-info-itemtitle">上映</div>
                <div class="video-info-item">上映：2025</div>
              </div>
              <div class="module-tab-item tab-item">线路一</div>
              <div class="module-list module-player-list">
                <div class="sort-item">
                  <a href="/vodplay/123-1-1.html">第1集</a>
                  <a href="/vodplay/123-1-1.html">第1集</a>
                  <a href="/vodplay/123-1-2.html">第2集</a>
                </div>
              </div>
            </body></html>
        """.trimIndent()

        val detail = parser.parseDetailPage(html, "https://vidhub3.top/voddetail/123.html")

        assertNotNull(detail)
        assertEquals("123", detail!!.id)
        assertEquals("测试影片", detail.title)
        assertEquals("2025", detail.year)
        assertEquals(1, detail.playSources.size)
        assertEquals(2, detail.playSources[0].episodes.size)
        assertEquals(1, detail.playSources[0].episodes[0].index)
        assertEquals(2, detail.playSources[0].episodes[1].index)
    }

    @Test
    fun `play page iframe is extracted even when relative`() {
        val html = """<html><body><iframe id="player_if" src="/player/?u=abc%2Bdef&i=1&n=1"></iframe></body></html>"""
        val src = parser.parsePlayPageForIframe(html)
        assertEquals("/player/?u=abc%2Bdef&i=1&n=1", src)
    }

    @Test
    fun `resolve response maps hls url`() {
        val json = """{"code":200,"url":"https:\/\/v.example.com\/a\/index.m3u8","from":"lzm3u8"}"""
        val resolved = parser.parseResolveResponse(json)
        assertNotNull(resolved)
        assertEquals("https://v.example.com/a/index.m3u8", resolved!!.url)
        assertEquals(PlayType.HLS, resolved.type)
    }

    @Test
    fun `resolve response rejects error code`() {
        assertNull(parser.parseResolveResponse("""{"code":-1,"msg":"fail"}"""))
    }

    @Test
    fun `suggest response maps items and unescapes image url`() {
        val json = """
            {"code":1,"total":2,"list":[
              {"id":269371,"name":"浪浪山小妖怪","pic":"https:\/\/img.example.com\/a.jpg"},
              {"id":263906,"name":"另一部","pic":"//img.example.com/b.jpg"}
            ]}
        """.trimIndent()

        val items = parser.parseSuggestResponse(json)

        assertEquals(2, items.size)
        assertEquals("269371", items[0].id)
        assertEquals("浪浪山小妖怪", items[0].title)
        assertEquals("https://img.example.com/a.jpg", items[0].coverUrl)
        assertEquals("https://img.example.com/b.jpg", items[1].coverUrl)
    }

    @Test
    fun `danmaku supports object and legacy array payloads`() {
        val json = """
            {"code":23,"danmuku":[
              {"time":1.5,"type":0,"color":16777215,"text":"前方高能"},
              [12.0,0,16711680,"旧格式"]
            ]}
        """.trimIndent()

        val danmaku = parser.parseDanmakuResponse(json)

        assertEquals(2, danmaku.size)
        assertEquals("前方高能", danmaku[0].text)
        assertEquals(1.5f, danmaku[0].time, 0.001f)
        assertEquals(0xFFFFFF, danmaku[0].color)
        assertEquals("旧格式", danmaku[1].text)
        assertEquals(0xFF0000, danmaku[1].color)
    }

    @Test
    fun `category page with no next link reports no more`() {
        val html = """<html><body><div class="module-item">
            <a href="/voddetail/1.html"><img data-src="https://img.example.com/1.jpg"></a>
            <div class="video-name"><a href="/voddetail/1.html">A</a></div>
        </div></body></html>"""
        val data = parser.parseCategoryPage(html, com.y_zion.daiflix.data.model.Category.MOVIE)
        assertTrue(data.items.isNotEmpty())
        assertTrue(!data.hasMore)
    }
}
