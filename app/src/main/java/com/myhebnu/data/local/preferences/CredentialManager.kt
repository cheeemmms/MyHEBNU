package com.myhebnu.data.local.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Securely stores student ID and plaintext password using EncryptedSharedPreferences.
 * Used for auto-login when the教务 session expires (HTTP 302).
 *
 * Security: MasterKeys.AES256_GCM + EncryptedSharedPreferences.
 * Credentials never leave the device and are only decrypted in memory during auto-login.
 */
@Singleton
class CredentialManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveCredentials(studentId: String, password: String) {
        prefs.edit()
            .putString(KEY_STUDENT_ID, studentId)
            .putString(KEY_PASSWORD, password)
            .apply()
    }

    fun loadCredentials(): Pair<String, String>? {
        val id = prefs.getString(KEY_STUDENT_ID, null) ?: return null
        val pw = prefs.getString(KEY_PASSWORD, null) ?: return null
        return Pair(id, pw)
    }

    fun hasCredentials(): Boolean {
        return prefs.contains(KEY_STUDENT_ID) && prefs.contains(KEY_PASSWORD)
    }

    fun clearCredentials() {
        prefs.edit()
            .remove(KEY_STUDENT_ID)
            .remove(KEY_PASSWORD)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "myhebnu_credentials"
        private const val KEY_STUDENT_ID = "student_id"
        private const val KEY_PASSWORD = "password"
    }
}
