package com.myhebnu.data.remote

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CookieJar that persists cookies both in-memory (for the current session)
 * and to encrypted storage (for auto-login across app restarts).
 */
@Singleton
class PersistentCookieJar @Inject constructor(
    private val sessionManager: SessionManager
) : CookieJar {

    // In-memory cache for active session
    private val cookieStore = ConcurrentHashMap<String, MutableList<Cookie>>()

    init {
        // Restore persisted cookies on creation
        restoreFromStorage()
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookieStore[url.host] = cookies.toMutableList()

        // Persist important cookies to encrypted storage
        val cookieMap = mutableMapOf<String, String>()
        for (host in cookieStore.keys) {
            cookieStore[host]?.forEach { cookie ->
                cookieMap["${host}_${cookie.name}"] = cookie.value
            }
        }
        if (cookieMap.isNotEmpty()) {
            sessionManager.saveCookies(cookieMap)
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return cookieStore[url.host] ?: emptyList()
    }

    /**
     * Check if there are any stored cookies (i.e. a prior session exists).
     */
    fun hasStoredSession(): Boolean {
        return sessionManager.hasValidSession()
    }

    /**
     * Clear all in-memory and persisted cookies (logout).
     */
    fun clearAll() {
        cookieStore.clear()
        sessionManager.clearSession()
    }

    private fun restoreFromStorage() {
        val storedCookies = sessionManager.loadCookies()
        for ((key, value) in storedCookies) {
            val parts = key.split("_", limit = 2)
            if (parts.size == 2) {
                val host = parts[0]
                val name = parts[1]
                val cookie = Cookie.Builder()
                    .name(name)
                    .value(value)
                    .domain(host)
                    .path("/")
                    .build()
                cookieStore.getOrPut(host) { mutableListOf() }.add(cookie)
            }
        }
    }
}
