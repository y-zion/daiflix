package com.y_zion.daiflix.data.api

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 播放器解析 API。
 *
 * 播放页 iframe 为 /player/?u={encrypted}...，解析接口为
 * GET /player/api/resolve.php?u={encrypted}，返回 {"code":200,"url":"..."}。
 * 旧版 /api/resolve.php?url=&ac=1 会返回 System Error，已废弃。
 */
interface ResolveApi {

    @GET("/player/api/resolve.php")
    suspend fun resolve(
        @Query("u") encryptedUrl: String
    ): String
}
