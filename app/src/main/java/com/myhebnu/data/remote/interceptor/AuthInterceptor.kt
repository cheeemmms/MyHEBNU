package com.myhebnu.data.remote.interceptor

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp Interceptor that:
 * 1. Masks OkHttp requests as browser AJAX calls (教务系统 blocks non-browser requests)
 * 2. Detects session expiry (302 → CAS login / 401 / 403)
 */
@Singleton
class AuthInterceptor @Inject constructor() : Interceptor {

    private val _sessionExpired = MutableStateFlow(false)
    val sessionExpired: StateFlow<Boolean> = _sessionExpired.asStateFlow()

    companion object {
        private const val UA = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/148.0.0.0 Mobile Safari/537.36"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val host = original.url.host

        // Determine Referer: each教务模块 checks that the caller comes from its own page.
        // Wrong module Referer → "无功能权限" HTML response.
        val path = original.url.encodedPath
        val referer = when {
            // N2154 周次课表 (Mobile + Zccx paths)
            path.contains("/kbcx/xskbcxZccx") || path.contains("/kbcx/xskbcxMobile") ->
                "http://$host/kbcx/xskbcxZccx_cxXskbcxIndex.html?gnmkdm=N2154&layout=default"
            // N2151 学期课表 (default kbcx)
            path.contains("/kbcx/") ->
                "http://$host/kbcx/xskbcx_cxXskbcxIndex.html?gnmkdm=N2151&layout=default"
            // N305007 成绩
            path.contains("/cjcx/") ->
                "http://$host/cjcx/cjcx_cxXskbcxIndex.html?gnmkdm=N305007&layout=default"
            // N2155 空教室
            path.contains("/cdjy/") ->
                "http://$host/cdjy/cdjy_cxKxcdlb.html?gnmkdm=N2155"
            // 其他 (菜单/登录等)
            else -> "http://$host/xtgl/index_initMenu.html?jsdm=xs"
        }

        val request = original.newBuilder()
            .header("User-Agent", UA)
            .header("X-Requested-With", "XMLHttpRequest")
            .header("Accept", "*/*")
            .header("Origin", "http://$host")
            .header("Referer", referer)
            .build()

        val response = chain.proceed(request)

        // Check for session expiry signals
        if (isSessionExpired(response)) {
            _sessionExpired.value = true
        }

        return response
    }

    private fun isSessionExpired(response: Response): Boolean {
        val location = response.header("Location") ?: ""
        if (response.code == 302 &&
            (location.contains("cas/login") || location.contains("login_slogin"))
        ) {
            return true
        }
        if (response.code == 401 || response.code == 403) {
            return true
        }
        return false
    }

    fun resetExpiredFlag() {
        _sessionExpired.value = false
    }
}
