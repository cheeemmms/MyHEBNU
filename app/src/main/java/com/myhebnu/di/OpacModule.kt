package com.myhebnu.di

import com.myhebnu.data.remote.PersistentCookieJar
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

/**
 * 图书馆（汇文 OPAC）网络层。
 *
 * 为什么单独一套 client，而不是复用 [NetworkModule.provideOkHttpClient]：
 * 1. 教务的 [com.myhebnu.data.remote.interceptor.AuthInterceptor] 会给**所有** host
 *    强塞教务的 Referer / Origin / X-Requested-With，并会把 CAS 的
 *    `302 → cas/login` 误判成教务会话过期，污染全局会话状态；
 * 2. OPAC 需要自己的 `X-Hw-*` 头；
 * 3. 两者共用**同一个** [PersistentCookieJar]——Cookie 按 host 分桶，
 *    所以图书馆与教务实为两套独立会话，互不影响。
 */
@Module
@InstallIn(SingletonComponent::class)
object OpacModule {

    const val BASE_URL = "https://hwdw.hebtu.edu.cn/meta-local/opac/"

    private const val UA =
        "Mozilla/5.0 (Linux; Android 14; Pixel) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/148.0.0.0 Mobile Safari/537.36"

    @Provides
    @Singleton
    @Named("opac")
    fun provideOpacOkHttpClient(cookieJar: PersistentCookieJar): OkHttpClient {
        return OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", UA)
                    .header("Accept", "application/json, text/plain, */*")
                    .header("X-Hw-Lang", "i18n.zh-cn")
                    .header("X-Hw-explorer", "m")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            // 登录链路需要逐跳观察 302，统一关闭自动跟随
            .followRedirects(false)
            .followSslRedirects(false)
            .build()
    }

    @Provides
    @Singleton
    @Named("opac")
    fun provideOpacRetrofit(@Named("opac") client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideOpacApi(@Named("opac") retrofit: Retrofit): com.myhebnu.data.remote.opac.OpacApi {
        return retrofit.create(com.myhebnu.data.remote.opac.OpacApi::class.java)
    }
}
