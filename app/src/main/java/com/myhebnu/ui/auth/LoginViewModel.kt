package com.myhebnu.ui.auth

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myhebnu.data.local.preferences.CredentialManager
import com.myhebnu.data.repository.AuthRepository
import com.myhebnu.data.repository.LoginResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val captchaInput: String = "",
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val loginUrl: String = "",
    val errorMessage: String? = null,
    val showCaptcha: Boolean = false,
    val captchaBitmap: ImageBitmap? = null,
    val showWebViewFallback: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val credentialManager: CredentialManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private var failureCount = 0

    companion object {
        private const val MAX_FAILURES_BEFORE_CAPTCHA = 3
    }

    init {
        checkLoginState()
    }

    private fun checkLoginState() {
        viewModelScope.launch {
            val hasSession = authRepository.hasValidSession()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isLoggedIn = hasSession,
                    loginUrl = if (!hasSession) authRepository.getLoginUrl() else ""
                )
            }
            // Pre-fill saved credentials if available
            if (!hasSession) {
                val creds = credentialManager.loadCredentials()
                if (creds != null) {
                    _uiState.update {
                        it.copy(username = creds.first, password = creds.second)
                    }
                }
            }
        }
    }

    fun onUsernameChanged(value: String) {
        _uiState.update { it.copy(username = value) }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update { it.copy(password = value) }
    }

    fun onCaptchaChanged(value: String) {
        _uiState.update { it.copy(captchaInput = value) }
    }

    fun login() {
        val state = _uiState.value
        if (state.username.isBlank()) {
            _uiState.update { it.copy(errorMessage = "请输入学号") }
            return
        }
        if (state.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "请输入密码") }
            return
        }
        if (state.showCaptcha && state.captchaInput.isBlank()) {
            _uiState.update { it.copy(errorMessage = "请输入验证码") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val captcha = if (state.showCaptcha) state.captchaInput else null
            val result: LoginResult = authRepository.performLogin(
                username = state.username,
                password = state.password,
                captcha = captcha
            )

            if (result.success) {
                failureCount = 0
                _uiState.update {
                    it.copy(isLoading = false, isLoggedIn = true, showCaptcha = false,
                        captchaBitmap = null, captchaInput = "")
                }
            } else {
                failureCount++
                val needsCaptcha = result.needsCaptcha || failureCount >= MAX_FAILURES_BEFORE_CAPTCHA
                var newBitmap: ImageBitmap? = null
                if (needsCaptcha) {
                    // Load captcha via loginClient (shares CookieJar) so server
                    // can associate the captcha answer with our session.
                    val bytes = authRepository.loadCaptchaImage()
                    newBitmap = if (bytes != null) {
                        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        bmp?.asImageBitmap()
                    } else null
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = result.errorMessage,
                        showCaptcha = needsCaptcha,
                        captchaBitmap = newBitmap,
                        captchaInput = ""
                    )
                }
            }
        }
    }

    fun refreshCaptcha() {
        viewModelScope.launch {
            val bytes = authRepository.loadCaptchaImage()
            if (bytes != null) {
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                _uiState.update { it.copy(captchaBitmap = bmp?.asImageBitmap()) }
            }
        }
    }

    fun showWebViewFallback() {
        _uiState.update {
            it.copy(
                showWebViewFallback = true,
                loginUrl = authRepository.getLoginUrl()
            )
        }
    }

    fun hideWebViewFallback() {
        _uiState.update { it.copy(showWebViewFallback = false) }
    }

    fun setupWebViewLogin() {
        _uiState.update {
            it.copy(
                showWebViewFallback = true,
                loginUrl = authRepository.getLoginUrl()
            )
        }
    }

    // === WebView callbacks (for fallback) ===

    fun onWebViewUrlChanged(url: String) {
        if (authRepository.isLoginSuccessUrl(url)) {
            viewModelScope.launch {
                try {
                    authRepository.onWebViewLoginSuccess()
                    // Also save credentials from the custom login fields if available
                    val state = _uiState.value
                    if (state.username.isNotBlank() && state.password.isNotBlank()) {
                        credentialManager.saveCredentials(state.username, state.password)
                    }
                    _uiState.update {
                        it.copy(isLoggedIn = true, showWebViewFallback = false)
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(errorMessage = e.message ?: "登录失败")
                    }
                }
            }
        }
    }

    fun onWebViewError(errorDescription: String?) {
        _uiState.update {
            it.copy(errorMessage = errorDescription ?: "登录失败")
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
