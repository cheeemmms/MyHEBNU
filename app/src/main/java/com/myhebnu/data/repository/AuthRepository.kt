package com.myhebnu.data.repository

import android.webkit.CookieManager
import com.myhebnu.data.local.preferences.UserPreferences
import com.myhebnu.data.remote.PersistentCookieJar
import com.myhebnu.data.remote.interceptor.AuthInterceptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles SSO authentication via CAS WebView login and session management.
 *
 * Login flow:
 * 1. WebView loads CAS login page
 * 2. User enters credentials → SSO redirect chain completes in WebView
 * 3. On success (URL reaches index_initMenu.html), extract cookies from WebView CookieManager
 * 4. Transfer cookies to OkHttp PersistentCookieJar for subsequent API calls
 * 5. Persist login state in UserPreferences
 */
@Singleton
class AuthRepository @Inject constructor(
    private val cookieJar: PersistentCookieJar,
    private val sessionManager: com.myhebnu.data.remote.SessionManager,
    private val preferences: UserPreferences,
    private val authInterceptor: AuthInterceptor,
    private val okHttpClient: OkHttpClient
) {
    companion object {
        // Mobile-friendly login page
        const val LOGIN_URL = "http://jwgl.hebtu.edu.cn/xtgl/login_slogin.html?ydType=0"

        // Success indicator URL after login completes
        const val LOGIN_SUCCESS_PATH = "/xtgl/index_initMenu.html"

        // Additional success indicators
        val LOGIN_SUCCESS_INDICATORS = listOf(
            LOGIN_SUCCESS_PATH
        )

        // Domain for cookie transfer
        const val JWGL_DOMAIN = "jwgl.hebtu.edu.cn"
        const val CAS_DOMAIN = "cas.hebtu.edu.cn"
    }

    /**
     * Check if a valid session exists (from stored encrypted cookies).
     */
    suspend fun hasValidSession(): Boolean {
        val hasCookieSession = cookieJar.hasStoredSession()
        val isLoggedInPref = preferences.isLoggedIn.first()
        android.util.Log.w("MyHEBNU", "hasValidSession check: hasCookieSession=$hasCookieSession, isLoggedInPref=$isLoggedInPref")
        
        if (!hasCookieSession || !isLoggedInPref) {
            return false
        }
        
        // 实际验证一下 cookie 是否有效
        return verifySessionValidity()
    }

    /**
     * 通过一个简单的请求验证 session 是否真的有效
     */
    private suspend fun verifySessionValidity(): Boolean = withContext(Dispatchers.IO) {
        try {
            android.util.Log.w("MyHEBNU", "正在验证 session 有效性...")
            val request = Request.Builder()
                .url("http://$JWGL_DOMAIN/xtgl/index_initMenu.html")
                .get()
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            val responseUrl = response.request.url.toString()
            val isSuccess = !responseUrl.contains("login_slogin.html")
            
            android.util.Log.w("MyHEBNU", "Session 验证结果: isSuccess=$isSuccess, responseUrl=$responseUrl")
            
            // 如果重定向到了登录页，说明 session 无效
            if (!isSuccess) {
                logout()
            }
            
            response.close()
            isSuccess
        } catch (e: Exception) {
            android.util.Log.e("MyHEBNU", "验证 session 时出错", e)
            // 网络错误时暂时返回 false，让用户重新登录
            false
        }
    }

    /**
     * Called after WebView login succeeds.
     * Extracts cookies from WebView CookieManager and transfers them to OkHttp CookieJar.
     */
    suspend fun onWebViewLoginSuccess() {
        android.util.Log.w("MyHEBNU", "=== onWebViewLoginSuccess 开始 ===")
        val cookieManager = CookieManager.getInstance()

        // Extract cookies from WebView for both domains
        val jwglCookies = cookieManager.getCookie("http://$JWGL_DOMAIN/") ?: ""
        val casCookies = cookieManager.getCookie("http://$CAS_DOMAIN/") ?: ""
        
        android.util.Log.w("MyHEBNU", "从WebView获取的cookies:")
        android.util.Log.w("MyHEBNU", "  jwglCookies=$jwglCookies")
        android.util.Log.w("MyHEBNU", "  casCookies=$casCookies")

        // Transfer CAS domain cookies
        transferCookies(casCookies, CAS_DOMAIN)

        // Transfer教务 system cookies
        transferCookies(jwglCookies, JWGL_DOMAIN)

        // Persist to encrypted storage
        val allCookies = mutableMapOf<String, String>()
        parseCookieString(jwglCookies).forEach { (name, value) ->
            allCookies["${JWGL_DOMAIN}_$name"] = value
        }
        parseCookieString(casCookies).forEach { (name, value) ->
            allCookies["${CAS_DOMAIN}_$name"] = value
        }
        
        android.util.Log.w("MyHEBNU", "准备保存到SessionManager的cookies: $allCookies")
        sessionManager.saveCookies(allCookies)

        preferences.setLoggedIn(true)
        authInterceptor.resetExpiredFlag()
        android.util.Log.w("MyHEBNU", "=== onWebViewLoginSuccess 完成 ===")
    }

    /**
     * Check if a URL indicates successful login.
     */
    fun isLoginSuccessUrl(url: String): Boolean {
        return LOGIN_SUCCESS_INDICATORS.any { url.contains(it) }
    }

    /**
     * Get the CAS login URL for WebView.
     */
    fun getLoginUrl(): String = LOGIN_URL

    /**
     * Logout: clear all cookies, stored session, and preferences.
     */
    suspend fun logout() {
        // Clear WebView cookies
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies(null)
        cookieManager.flush()

        // Clear OkHttp cookies
        cookieJar.clearAll()

        // Clear preferences
        preferences.setLoggedIn(false)
        authInterceptor.resetExpiredFlag()
    }

    /**
     * Handle session expiry detected by AuthInterceptor.
     * Attempts silent recovery, or signals for re-login if not possible.
     */
    suspend fun handleSessionExpired(): SessionExpiryAction {
        return if (cookieJar.hasStoredSession()) {
            // Try to use stored cookies (they might still be valid against a different endpoint)
            SessionExpiryAction.SilentRetry
        } else {
            // No stored session, need full re-login
            SessionExpiryAction.RequireLogin
        }
    }

    private fun transferCookies(cookieString: String, domain: String) {
        val cookies = parseCookieString(cookieString)
        val url = "http://$domain/".toHttpUrl()
        for ((name, value) in cookies) {
            val cookie = Cookie.Builder()
                .name(name)
                .value(value)
                .domain(domain)
                .path("/")
                .build()
            cookieJar.saveFromResponse(url, listOf(cookie))
        }
    }

    private fun parseCookieString(cookieString: String): Map<String, String> {
        if (cookieString.isBlank()) return emptyMap()
        val result = mutableMapOf<String, String>()
        cookieString.split(";").forEach { part ->
            val trimmed = part.trim()
            val eqIndex = trimmed.indexOf('=')
            if (eqIndex > 0) {
                val name = trimmed.substring(0, eqIndex)
                val value = trimmed.substring(eqIndex + 1)
                result[name] = value
            }
        }
        return result
    }
}

enum class SessionExpiryAction {
    /** Stored cookies exist, retry the request first */
    SilentRetry,
    /** No stored session, must show login UI */
    RequireLogin
}
