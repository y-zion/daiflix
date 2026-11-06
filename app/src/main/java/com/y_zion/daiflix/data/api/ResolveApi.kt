package com.y_zion.daiflix.data.api

import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * 视频解析 API
 * 将 VidHub 的播放页面 URL 解析为实际的视频流地址
 */
interface ResolveApi {

    /**
     * 解析视频播放地址
     * @param url 播放页面 URL
     * @return JSON 响应，包含实际播放 URL
     */
    @GET("/api/resolve.php")
    suspend fun resolve(
        @Query("url") url: String,
        @Query("ac") ac: Int = 1
    ): String

    /**
     * 备用解析接口
     */
    @GET
    suspend fun resolveAlt(
        @Url url: String
    ): String
}
