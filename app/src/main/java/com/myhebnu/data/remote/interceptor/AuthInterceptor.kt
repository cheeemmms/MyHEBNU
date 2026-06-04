package com.myhebnu.data.remote.interceptor

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp Interceptor that detects session expiry and emits a signal
 * so the UI layer can trigger re-authentication.
 *
 * Session expiry is detected by:
 * - HTTP 302 redirect to CAS login URL (most common for this教务系统)
 * - Response body containing login page markers
 */
@Singleton
class AuthInterceptor @Inject constructor() : Interceptor {

    private val _sessionExpired = MutableStateFlow(false)
    val sessionExpired: StateFlow<Boolean> = _sessionExpired.asStateFlow()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        // Check for session expiry signals
        if (isSessionExpired(response)) {
            _sessionExpired.value = true
        }

        return response
    }

    private fun isSessionExpired(response: Response): Boolean {
        // 302 redirect to CAS login page = session expired
        val location = response.header("Location") ?: ""
        if (response.code == 302 &&
            (location.contains("cas/login") || location.contains("login_slogin"))
        ) {
            return true
        }

        // 401/403 status
        if (response.code == 401 || response.code == 403) {
            return true
        }

        return false
    }

    /**
     * Reset the expired flag after a successful re-login.
     */
    fun resetExpiredFlag() {
        _sessionExpired.value = false
    }
}
