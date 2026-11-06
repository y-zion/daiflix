package com.y_zion.daiflix.di

import com.y_zion.daiflix.data.api.DanmakuApi
import com.y_zion.daiflix.data.api.ResolveApi
import com.y_zion.daiflix.data.api.VidHubApi
import com.y_zion.daiflix.data.local.AppPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(preferences: AppPreferences): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(HeaderInterceptor(preferences))
            .addInterceptor(RewriteUrlInterceptor(preferences))
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://vidhub3.top/")
            .client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideVidHubApi(retrofit: Retrofit): VidHubApi {
        return retrofit.create(VidHubApi::class.java)
    }

    @Provides
    @Singleton
    fun provideResolveApi(retrofit: Retrofit): ResolveApi {
        return retrofit.create(ResolveApi::class.java)
    }

    @Provides
    @Singleton
    fun provideDanmakuApi(retrofit: Retrofit): DanmakuApi {
        return retrofit.create(DanmakuApi::class.java)
    }
}

/**
 * 添加浏览器 UA，避免被反爬
 */
class HeaderInterceptor(private val preferences: AppPreferences) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val referer = preferences.baseUrl.trimEnd('/') + "/"
        val request = chain.request().newBuilder()
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
            .header("Referer", referer)
            .build()
        return chain.proceed(request)
    }
}

/**
 * 动态重写请求 URL，支持用户自定义域名（含子路径部署）。
 * 旧实现只做字符串替换 "https://vidhub3.top"，换域名后所有请求仍打原站。
 */
class RewriteUrlInterceptor(private val preferences: AppPreferences) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        // Retrofit 的 baseUrl 固定为默认站点；只重写默认站点的请求，
        // 避免把播放器 iframe/CDN 的绝对地址误改到用户配置的域名。
        if (original.url.host != "vidhub3.top") {
            return chain.proceed(original)
        }
        val base = preferences.baseUrl.trimEnd('/').toHttpUrlOrNull()
            ?: return chain.proceed(original)

        val prefix = base.encodedPath.trimEnd('/')
        val builder = original.url.newBuilder()
            .scheme(base.scheme)
            .host(base.host)
            .port(base.port)
        if (prefix.isNotEmpty()) {
            builder.encodedPath(prefix + original.url.encodedPath)
        }

        return chain.proceed(original.newBuilder().url(builder.build()).build())
    }
}
