package com.myhebnu.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myhebnu.data.remote.opac.OpacRequest
import com.myhebnu.data.remote.opac.OpacStats
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

data class FinesUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val records: List<OpacRequest> = emptyList(),
    val stats: OpacStats? = null,
) {
    val isEmpty: Boolean get() = !isLoading && error == null && records.isEmpty()

    /** 汇总金额：优先用 stats.fineSum，缺失时退回记录条数。 */
    val totalFine: Double get() = stats?.fineSum ?: 0.0

    val overdueCount: Int get() = stats?.expireCount ?: 0
}

/** 罚金与违约。抓包里样本恒为空列表，所以这里以「空态友好」为主。 */
@HiltViewModel
class FinesViewModel @Inject constructor(
    private val repository: OpacRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FinesUiState())
    val uiState: StateFlow<FinesUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = runCatching {
                coroutineScope {
                    val records = async { repository.fineRecords() }
                    val stats = async { repository.stats() }
                    val r = records.await()
                    val s = stats.await()
                    if (r.isFailure) {
                        throw r.exceptionOrNull() ?: IllegalStateException("罚金记录加载失败")
                    }
                    r.getOrNull().orEmpty() to s.getOrNull()
                }
            }

            _uiState.update { state ->
                result.fold(
                    onSuccess = { (records, stats) ->
                        state.copy(isLoading = false, error = null, records = records, stats = stats)
                    },
                    onFailure = { error ->
                        state.copy(
                            isLoading = false,
                            error = error.message ?: "罚金记录加载失败",
                        )
                    },
                )
            }
        }
    }
}
