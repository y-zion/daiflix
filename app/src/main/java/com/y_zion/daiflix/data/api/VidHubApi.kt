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
     * 分类列表
     * @param categoryId 分类 ID: 1=电影, 2=连续剧, 3=综艺, 4=动漫
     * @param page 页码
     */
    @GET("/vodtype/{categoryId}.html")
    suspend fun getCategoryPage(
        @Path("categoryId") categoryId: String,
        @Query("page") page: Int = 1
    ): String

    /**
     * 搜索
     * @param keyword 搜索关键词
     */
    @GET("/vodsearch/{keyword}------------.html")
    suspend fun search(
        @Path("keyword") keyword: String
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
     * 播放器 iframe 页面（包含实际视频 URL）
     */
    @GET
    suspend fun getPlayerPage(
        @Url url: String
    ): String

    companion object {
        fun getCategoryPath(category: Category): String = category.value
    }
}
