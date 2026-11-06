package com.yzion.daiflix.tv

import android.app.Application
import android.graphics.drawable.ColorDrawable
import coil.ImageLoader
import coil.ImageLoaderFactory
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient

@HiltAndroidApp
class DaiflixApp : Application(), ImageLoaderFactory {

    /**
     * 站点封面走豆瓣图床：不带 Referer 返回 418/403。
     * 这里给 Coil 单独配置一个带 Referer 的 OkHttpClient，
     * 避免影响正片请求，也避免云盘 CDN 被无关 Header 干扰。
     */
    override fun newImageLoader(): ImageLoader {
        val imageClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                val referer = if (request.url.host.contains("douban")) {
                    "https://movie.douban.com/"
                } else {
                    "https://vidhub3.top/"
                }
                chain.proceed(
                    request.newBuilder()
                        .header("Referer", referer)
                        .header(
                            "User-Agent",
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                                "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                        )
                        .build()
                )
            }
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(imageClient)
            .crossfade(true)
            .placeholder(ColorDrawable(0xFF1A1A1A.toInt()))
            .error(ColorDrawable(0xFF2A2A2A.toInt()))
            .build()
    }
}
