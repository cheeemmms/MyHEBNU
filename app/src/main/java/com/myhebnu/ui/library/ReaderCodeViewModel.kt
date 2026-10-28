package com.myhebnu.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myhebnu.data.repository.OpacRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReaderCodeUiState(
    val code: String = "",
    val name: String = "",
    val studentId: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
    val secondsLeft: Int = REFRESH_SECONDS,
) {
    companion object {
        /** 与汇文前端 `space.m.ecard.refresh` 配置一致：30 秒换一次码。 */
        const val REFRESH_SECONDS = 30
    }
}

/**
 * 动态读者证（借书码）。
 *
 * `users/elec` 每次请求都返回一个全新的 14 位码——它是一次性令牌，不能缓存、不能复用，
 * 所以这里维持「取码 → 倒计时 → 自动换码」的循环，并在界面离开时停掉。
 */
@HiltViewModel
class ReaderCodeViewModel @Inject constructor(
    private val repository: OpacRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReaderCodeUiState())
    val uiState: StateFlow<ReaderCodeUiState> = _uiState.asStateFlow()

    private var ticker: Job? = null

    init {
        loadCode()
        startTicker()
    }

    /** 手动换码：立即取一张新的，并把倒计时重置。 */
    fun refresh() {
        if (_uiState.value.isLoading) return
        loadCode()
        startTicker()
    }

    private fun loadCode() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val codeResult = repository.readerCode()
            val profile = repository.profile().getOrNull()

            _uiState.update { state ->
                codeResult.fold(
                    onSuccess = { code ->
                        state.copy(
                            code = code,
                            name = profile?.displayName ?: state.name,
                            studentId = profile?.userId ?: state.studentId,
                            isLoading = false,
                            error = null,
                            secondsLeft = ReaderCodeUiState.REFRESH_SECONDS,
                        )
                    },
                    onFailure = { error ->
                        state.copy(
                            isLoading = false,
                            error = error.message ?: "读者码获取失败",
                        )
                    },
                )
            }
        }
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (isActive) {
                delay(1_000)
                val state = _uiState.value
                if (state.code.isBlank()) continue
                val left = state.secondsLeft - 1
                if (left > 0) {
                    _uiState.update { it.copy(secondsLeft = left) }
                } else {
                    _uiState.update { it.copy(secondsLeft = ReaderCodeUiState.REFRESH_SECONDS) }
                    loadCode()
                }
            }
        }
    }

    override fun onCleared() {
        ticker?.cancel()
        super.onCleared()
    }
}
