package com.myhebnu.data.repository

import com.myhebnu.data.local.preferences.UserPreferences
import com.myhebnu.data.remote.EASystemApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: EASystemApi,
    private val preferences: UserPreferences
) {
    // Phase 2 will implement:
    // - SSO login via WebView (CAS RSA encryption + ticket exchange)
    // - Session validation
    // - Auto-login with stored cookies
    // - Silent re-login on session expiry
    // - Logout (clear cookies + preferences)

    suspend fun isLoggedIn(): Boolean {
        // TODO: Phase 2 — check stored session validity
        return false
    }

    suspend fun logout() {
        // TODO: Phase 2 — clear cookies and preferences
        preferences.setLoggedIn(false)
    }
}
