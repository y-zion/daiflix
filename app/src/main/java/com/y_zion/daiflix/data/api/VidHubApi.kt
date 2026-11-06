package com.y_zion.daiflix.data.api

import com.y_zion.daiflix.data.model.Category
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * VidHub 网页版 API（HTML 页面抓取）
 * 基础 URL 可配置，默认 https://vidhub3.top
 */
interface VidHubApi {

    /**
     * 首页（获取推荐、热门内容）
     */
    @GET("/")
    suspend fun getHomePage(): String

    /**
     * 分类列表。站点本身只提供第一页（/vodtype/1.html），
     * 没有可靠的第二页，因此不再伪造 page 查询参数。
     */
    @GET("/vodtype/{categoryId}.html")
    suspend fun getCategoryPage(
        @Path("categoryId") categoryId: String
    ): String

    /**
     * 搜索建议接口。网页搜索页 /vodsearch/... 被人机验证拦截，
     * 但 /ajax/suggest 返回 JSON，可直接作为搜索结果使用。
     */
    @GET("/ajax/suggest")
    suspend fun suggest(
        @Query("wd") keyword: String,
        @Query("mid") mid: Int = 1,
        @Query("limit") limit: Int = 30
    ): String

    /**
     * 视频详情页
     * @param id 视频 ID
     */
    @GET("/voddetail/{id}.html")
    suspend fun getDetail(
        @Path("id") id: String
    ): String

    /**
     * 播放页（包含播放器 iframe）
     * URL 格式: /vodplay/{id}-{sourceId}-{episodeId}.html
     */
    @GET("/vodplay/{id}-{sourceId}-{episodeId}.html")
    suspend fun getPlayPage(
        @Path("id") id: String,
        @Path("sourceId") sourceId: Int = 1,
        @Path("episodeId") episodeId: Int = 1
    ): String

    /**
     * 播放器 iframe 页面。正常情况下播放地址由 /player/api/resolve.php 解析，
     * 此接口用于兼容 config 中直接带 url 的少量页面。
     */
    @GET
    suspend fun getPlayerPage(
        @Url url: String
    ): String

    companion object {
        fun getCategoryPath(category: Category): String = category.value
    }
}
