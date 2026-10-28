package com.myhebnu.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myhebnu.data.remote.opac.OpacRequest
import com.myhebnu.data.repository.OpacRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class RequestsTab { Reservations, Suggestions }

data class RequestsUiState(
    val tab: RequestsTab = RequestsTab.Reservations,
    val isLoading: Boolean = true,
    val error: String? = null,
    val reservations: List<OpacRequest> = emptyList(),
    val suggestions: List<OpacRequest> = emptyList(),
) {
    val current: List<OpacRequest>
        get() = when (tab) {
            RequestsTab.Reservations -> reservations
            RequestsTab.Suggestions -> suggestions
        }

    val isEmpty: Boolean get() = !isLoading && error == null && current.isEmpty()
}

/**
 * 预约 / 委托 与 荐购记录。
 *
 * 本期只读：提交预约（`requests/item_copies`）与提交荐购都没有成功样本，
 * 所以这里不提供任何写操作入口。
 */
@HiltViewModel
class RequestsViewModel @Inject constructor(
    private val repository: OpacRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RequestsUiState())
    val uiState: StateFlow<RequestsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun selectTab(tab: RequestsTab) {
        _uiState.update { it.copy(tab = tab) }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.current.isEmpty(), error = null) }

            val result = runCatching {
                coroutineScope {
                    val reservations = async { repository.activeRequests() }
                    val suggestions = async { repository.suggestions() }
                    val r = reservations.await()
                    val s = suggestions.await()
                    if (r.isFailure && s.isFailure) {
                        throw r.exceptionOrNull()
                            ?: s.exceptionOrNull()
                            ?: IllegalStateException("预约记录加载失败")
                    }
                    r.getOrNull().orEmpty() to s.getOrNull().orEmpty()
                }
            }

            _uiState.update { state ->
                result.fold(
                    onSuccess = { (reservations, suggestions) ->
                        state.copy(
                            isLoading = false,
                            error = null,
                            reservations = reservations,
                            suggestions = suggestions,
                        )
                    },
                    onFailure = { error ->
                        state.copy(
                            isLoading = false,
                            error = error.message ?: "预约记录加载失败",
                        )
                    },
                )
            }
        }
    }
}
