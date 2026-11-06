package com.y_zion.daiflix.data.api

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 弹幕 API
 */
interface DanmakuApi {

    /**
     * 获取弹幕
     * @param vid 视频 ID
     * @return JSON 格式的弹幕数据
     */
    @GET("/player/dmku/")
    suspend fun getDanmaku(
        @Query("vid") vid: String
    ): String
}
