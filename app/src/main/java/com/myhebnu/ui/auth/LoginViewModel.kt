package com.myhebnu.ui.auth

import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myhebnu.data.local.preferences.CredentialManager
import com.myhebnu.data.local.preferences.UserPreferences
import com.myhebnu.data.repository.AuthRepository
import com.myhebnu.data.repository.LoginResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    val captchaBitmap: ImageBitmap? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val credentialManager: CredentialManager,
    private val preferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private var failureCount = 0

    companion object {
        private const val TAG = "LoginVM"
    }

    init {
        prefillSavedCredentials()
    }

    /**
     * 进入登录页时调用：重置错误与验证码输入，并向服务端确认这次要不要验证码。
     *
     * **验证码只由服务端决定**（`getKaptchaStatus`）。早期版本还自己数失败次数，
     * 数到第 3 次就强行弹验证码——那会在服务端并不要求时塞一个它不认的 authcode，
     * 反而让「密码错误」变成更难排查的错误。已删除。
     */
    fun onLoginScreenShown() {
        failureCount = 0
        _uiState.update {
            it.copy(
                errorMessage = null,
                captchaInput = "",
                showCaptcha = false,
                captchaBitmap = null
            )
        }
        viewModelScope.launch {
            val needsCaptcha = authRepository.startLoginSession()
            Log.w(TAG, "登录页就绪: 服务端要求验证码=$needsCaptcha")
            if (!needsCaptcha) return@launch
            val bytes = authRepository.loadCaptchaImage() ?: return@launch
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            _uiState.update { it.copy(showCaptcha = true, captchaBitmap = bitmap) }
        }
    }

    /**
     * Login 页只在 MainActivity 判定“无本地会话”后才创建，因此这里不再做任何
     * 服务端会话校验（那会重复一次网络请求并拖慢启动）。只回填本地加密凭证。
     */
    private fun prefillSavedCredentials() {
        viewModelScope.launch {
            val creds = credentialManager.loadCredentials() ?: return@launch
            // 证书来自旧登录方式时只回填学号：那个密码多半不是统一身份认证密码，
            // 预填进去反而容易让用户直接点登录、白费一次失败次数。
            val migrated = preferences.ssoCredentialsMigrated.first()
            _uiState.update { state ->
                // 回填是异步的：用户可能已经开始输入，绝不能覆盖已输入的内容
                state.copy(
                    username = state.username.ifBlank { creds.first },
                    password = if (state.password.isNotBlank()) state.password
                    else if (migrated) creds.second else ""
                )
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
            Log.w(TAG, "提交被拦截: 学号为空")
            _uiState.update { it.copy(errorMessage = "请输入学号") }
            return
        }
        if (state.password.isBlank()) {
            Log.w(TAG, "提交被拦截: 密码为空")
            _uiState.update { it.copy(errorMessage = "请输入密码") }
            return
        }
        if (state.showCaptcha && state.captchaInput.isBlank()) {
            Log.w(TAG, "提交被拦截: 服务端要求验证码但未填写")
            _uiState.update { it.copy(errorMessage = "请输入验证码") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val captcha = if (state.showCaptcha) state.captchaInput else null
            Log.w(TAG, "点击登录: 学号长度=${state.username.length}, 密码长度=${state.password.length}, 验证码=${captcha != null}")
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
                // 是否需要验证码完全由服务端决定（result.needsCaptcha），不再按本地失败次数硬凑
                val needsCaptcha = result.needsCaptcha
                var newBitmap: ImageBitmap? = null
                if (needsCaptcha) {
                    // 图必须和这次的表单同一会话，否则填了也不认
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

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * 登录成功回调已消费。LoginViewModel 是 Activity 级、会跨页面存活；
     * 若不清掉 isLoggedIn，下次会话过期回到登录页时会立刻再次触发成功回调。
     */
    fun consumeLoginSuccess() {
        _uiState.update { it.copy(isLoggedIn = false) }
    }
}
