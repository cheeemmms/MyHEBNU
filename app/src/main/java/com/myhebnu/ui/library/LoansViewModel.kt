package com.myhebnu.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myhebnu.data.remote.opac.OpacLoan
import com.myhebnu.data.repository.OpacRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoansUiState(
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val loans: List<OpacLoan> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val hasMore: Boolean = false,
    val quotaText: String = "",
) {
    val isEmpty: Boolean get() = !isLoading && error == null && loans.isEmpty()
}

/** 当前借阅列表。续借本期不做，这里只读。 */
@HiltViewModel
class LoansViewModel @Inject constructor(
    private val repository: OpacRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoansUiState())
    val uiState: StateFlow<LoansUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.loans.isEmpty(), error = null) }

            val loansResult = repository.loans(page = 1, pageSize = PAGE_SIZE)
            val quota = repository.loanQuota().getOrNull().orEmpty()

            _uiState.update { state ->
                loansResult.fold(
                    onSuccess = { page ->
                        state.copy(
                            isLoading = false,
                            error = null,
                            loans = page.items,
                            total = page.total,
                            page = 1,
                            hasMore = page.items.size < page.total,
                            quotaText = quota.ifBlank { state.quotaText },
                        )
                    },
                    onFailure = { error ->
                        state.copy(
                            isLoading = false,
                            error = error.message ?: "借阅记录加载失败",
                        )
                    },
                )
            }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || state.isLoading || !state.hasMore) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            val nextPage = state.page + 1
            val result = repository.loans(page = nextPage, pageSize = PAGE_SIZE)

            _uiState.update { current ->
                result.fold(
                    onSuccess = { page ->
                        val merged = current.loans + page.items
                        current.copy(
                            isLoadingMore = false,
                            loans = merged,
                            page = nextPage,
                            total = page.total,
                            hasMore = merged.size < page.total,
                        )
                    },
                    onFailure = { error ->
                        current.copy(
                            isLoadingMore = false,
                            error = error.message,
                        )
                    },
                )
            }
        }
    }

    private companion object {
        const val PAGE_SIZE = 20
    }
}
