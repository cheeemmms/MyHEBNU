package com.myhebnu.data.repository

import android.webkit.CookieManager
import com.myhebnu.data.local.preferences.UserPreferences
import com.myhebnu.data.remote.PersistentCookieJar
import com.myhebnu.data.remote.interceptor.AuthInterceptor
import kotlinx.coroutines.flow.first
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
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
    private val authInterceptor: AuthInterceptor
) {
    companion object {
        // CAS login page
        const val CAS_LOGIN_URL = "http://cas.hebtu.edu.cn/cas/login"

        // Success indicator URL after full SSO flow completes
        const val LOGIN_SUCCESS_PATH = "/xtgl/index_initMenu.html"

        // Additional success indicators
        val LOGIN_SUCCESS_INDICATORS = listOf(
            LOGIN_SUCCESS_PATH,
            "/xtgl/login_slogin.html"
        )

        // Domain for cookie transfer
        const val JWGL_DOMAIN = "jwgl.hebtu.edu.cn"
        const val CAS_DOMAIN = "cas.hebtu.edu.cn"
    }

    /**
     * Check if a valid session exists (from stored encrypted cookies).
     */
    suspend fun hasValidSession(): Boolean {
        return cookieJar.hasStoredSession() && preferences.isLoggedIn.first()
    }

    /**
     * Called after WebView login succeeds.
     * Extracts cookies from WebView CookieManager and transfers them to OkHttp CookieJar.
     */
    suspend fun onWebViewLoginSuccess() {
        val cookieManager = CookieManager.getInstance()

        // Extract cookies from WebView for both domains
        val jwglCookies = cookieManager.getCookie(JWGL_DOMAIN) ?: ""
        val casCookies = cookieManager.getCookie(CAS_DOMAIN) ?: ""

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
        sessionManager.saveCookies(allCookies)

        preferences.setLoggedIn(true)
        authInterceptor.resetExpiredFlag()
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
    fun getLoginUrl(): String = CAS_LOGIN_URL

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
