package com.myhebnu.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myhebnu.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val isLoading: Boolean = true,
    val isLoggedIn: Boolean = false,
    val loginUrl: String = "",
    val errorMessage: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

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
        }
    }

    fun onWebViewUrlChanged(url: String) {
        if (authRepository.isLoginSuccessUrl(url)) {
            viewModelScope.launch {
                try {
                    authRepository.onWebViewLoginSuccess()
                    _uiState.update { it.copy(isLoggedIn = true) }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(errorMessage = e.message ?: "Login failed")
                    }
                }
            }
        }
    }

    fun onLoginError(errorDescription: String?) {
        _uiState.update {
            it.copy(errorMessage = errorDescription ?: "Login failed")
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
