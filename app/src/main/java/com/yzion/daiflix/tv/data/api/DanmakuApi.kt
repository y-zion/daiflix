package com.yzion.daiflix.tv.data.api

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 弹幕 API。
 *
 * 站点实际接口为 GET /player/dmku/?id={videoId}&ac=get，
 * 返回 {"code":23,"name":"...","danum":0,"danmuku":[]}。
 * 旧代码使用 ?vid=，会返回 {"code":-1,"danmuku":"未知操作"}。
 */
interface DanmakuApi {

    @GET("/player/dmku/")
    suspend fun getDanmaku(
        @Query("id") id: String,
        @Query("ac") action: String = "get"
    ): String
}
