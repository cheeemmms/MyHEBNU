package com.myhebnu.data.remote

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists CAS SSO session cookies using EncryptedSharedPreferences.
 *
 * Cookies are stored in encrypted form to protect the session from
 * extraction on rooted devices or by malicious apps.
 */
@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

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
        // Session considered valid if cookies exist and were saved within 7 days
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
        private const val PREFS_NAME = "myhebnu_encrypted_session"
        private const val KEY_COOKIE_NAMES = "cookie_names"
        private const val KEY_COOKIE_PREFIX = "cookie_"
        private const val KEY_LAST_SAVED = "last_saved"
        private const val SESSION_MAX_AGE_MS = 7 * 24 * 60 * 60 * 1000L // 7 days
    }
}
