package com.myhebnu.data.remote

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists CAS SSO session cookies.
 * Note: Uses plain SharedPreferences for MVP. EncryptedSharedPreferences (Phase 8).
 */
@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveCookies(cookies: Map<String, String>) {
        prefs.edit().apply {
            putStringSet(KEY_COOKIE_NAMES, cookies.keys)
            cookies.forEach { (name, value) ->
                putString("${KEY_COOKIE_PREFIX}$name", value)
            }
            putLong(KEY_LAST_SAVED, System.currentTimeMillis())
        }.apply()
    }

    fun loadCookies(): Map<String, String> {
        val names = prefs.getStringSet(KEY_COOKIE_NAMES, emptySet()) ?: emptySet()
        val result = mutableMapOf<String, String>()
        for (name in names) {
            val value = prefs.getString("${KEY_COOKIE_PREFIX}$name", null)
            if (value != null) {
                result[name] = value
            }
        }
        return result
    }

    fun hasValidSession(): Boolean {
        val lastSaved = prefs.getLong(KEY_LAST_SAVED, 0)
        val cookieNames = prefs.getStringSet(KEY_COOKIE_NAMES, emptySet())
        return !cookieNames.isNullOrEmpty() &&
            System.currentTimeMillis() - lastSaved < SESSION_MAX_AGE_MS
    }

    fun clearSession() {
        val names = prefs.getStringSet(KEY_COOKIE_NAMES, emptySet()) ?: emptySet()
        prefs.edit().apply {
            for (name in names) {
                remove("${KEY_COOKIE_PREFIX}$name")
            }
            remove(KEY_COOKIE_NAMES)
            remove(KEY_LAST_SAVED)
        }.apply()
    }

    companion object {
        private const val PREFS_NAME = "myhebnu_session"
        private const val KEY_COOKIE_NAMES = "cookie_names"
        private const val KEY_COOKIE_PREFIX = "cookie_"
        private const val KEY_LAST_SAVED = "last_saved"
        private const val SESSION_MAX_AGE_MS = 7 * 24 * 60 * 60 * 1000L
    }
}
