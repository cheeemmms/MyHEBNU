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
        android.util.Log.w("MyHEBNU", "saveFromResponse: url=$url, cookies=${cookies.map { "${it.name}=${it.value}" }}")
        // Merge cookies instead of replacing — keeps JSESSIONID etc. across redirects
        val existing = cookieStore.getOrPut(url.host) { mutableListOf() }
        for (newCookie in cookies) {
            existing.removeAll { it.name == newCookie.name }
            existing.add(newCookie)
        }

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
        val cookies = cookieStore[url.host] ?: emptyList()
        android.util.Log.w("MyHEBNU", "loadForRequest: url=$url, cookies=${cookies.map { "${it.name}=${it.value}" }}")
        return cookies
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

    /**
     * 只清掉某个域名下的 Cookie，返回是否真的清掉了东西。
     *
     * 用途：统一身份认证域下残留的旧会话会让 CAS 把新表单判成「闲置时间过长」，
     * 或者直接回「密码错误」，所以重新登录前必须先丢掉这个域的旧 Cookie。
     */
    fun removeHost(host: String): Boolean {
        val inMemory = cookieStore.remove(host)?.isNotEmpty() == true
        val storedKeys = sessionManager.loadCookies().keys.filter { it.startsWith("${host}_") }
        if (storedKeys.isNotEmpty()) {
            sessionManager.removeCookies(storedKeys)
        }
        android.util.Log.w("MyHEBNU", "removeHost: host=$host, 内存=$inMemory, 持久化=${storedKeys.size} 个")
        return inMemory || storedKeys.isNotEmpty()
    }

    private fun restoreFromStorage() {
        val storedCookies = sessionManager.loadCookies()
        android.util.Log.w("MyHEBNU", "restoreFromStorage: 从SessionManager加载了 ${storedCookies.size} 个cookie")
        android.util.Log.w("MyHEBNU", "  内容: $storedCookies")
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
        android.util.Log.w("MyHEBNU", "restoreFromStorage: cookieStore现在有 ${cookieStore.keys.size} 个host的cookies")
    }
}
