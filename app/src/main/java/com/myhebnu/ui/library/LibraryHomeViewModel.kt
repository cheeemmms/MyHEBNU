package com.myhebnu.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myhebnu.data.remote.opac.OpacLoan
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

data class LibraryHomeUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val name: String = "",
    val studentId: String = "",
    val affiliation: String = "",
    val loanCount: Int = 0,
    val loanLimit: Int = 0,
    val expireCount: Int = 0,
    val requestCount: Int = 0,
    val fineSum: Double = 0.0,
    val credit: Int = 0,
    val categories: List<Pair<String, Int>> = emptyList(),
    val nextDue: OpacLoan? = null,
) {
    /** 借阅额度进度，0f–1f；上限未知时返回 null，界面就不画进度。 */
    val quotaProgress: Float?
        get() = if (loanLimit > 0) (loanCount.toFloat() / loanLimit).coerceIn(0f, 1f) else null
}

/**
 * 图书馆首页：只做「一屏看完当前状态」，不承载任何写操作。
 *
 * 五个接口并行请求，任何一个失败都不阻塞整页——已拿到的数据先显示，
 * 只有全部失败时才进入错误态（这样弱网下也能看到借书证入口）。
 */
@HiltViewModel
class LibraryHomeViewModel @Inject constructor(
    private val repository: OpacRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryHomeUiState())
    val uiState: StateFlow<LibraryHomeUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.name.isBlank(), error = null) }

            val result = runCatching {
                coroutineScope {
                    val profile = async { repository.profile() }
                    val stats = async { repository.stats() }
                    val quota = async { repository.loanQuota() }
                    val loans = async { repository.loans(page = 1, pageSize = 5) }
                    val categories = async { repository.loanCategories() }

                    val profileResult = profile.await()
                    val statsResult = stats.await()
                    val info = profileResult.getOrNull()
                    val stat = statsResult.getOrNull()
                    val quotaText = quota.await().getOrNull()
                    val loanPage = loans.await().getOrNull()
                    val cats = categories.await().getOrNull().orEmpty()

                    if (info == null && stat == null) {
                        val cause = profileResult.exceptionOrNull()
                            ?: statsResult.exceptionOrNull()
                            ?: IllegalStateException("图书馆数据加载失败")
                        throw cause
                    }

                    val (borrowed, limit) = parseQuota(quotaText)
                    LibraryHomeUiState(
                        isLoading = false,
                        error = null,
                        name = info?.displayName.orEmpty(),
                        studentId = info?.userId.orEmpty(),
                        affiliation = listOfNotNull(
                            info?.dept?.takeIf { it.isNotBlank() },
                            info?.userClass?.takeIf { it.isNotBlank() },
                        ).joinToString(" · "),
                        loanCount = borrowed ?: stat?.loanCount ?: 0,
                        loanLimit = limit ?: 0,
                        expireCount = stat?.expireCount ?: 0,
                        requestCount = stat?.requestCount ?: 0,
                        fineSum = stat?.fineSum ?: 0.0,
                        credit = info?.credit ?: 0,
                        categories = cats.take(4).map { it.category.orEmpty() to (it.count ?: 0) },
                        nextDue = loanPage?.items.orEmpty()
                            .filter { it.dueDate != null }
                            .minByOrNull { it.dueDate.orEmpty() },
                    )
                }
            }

            _uiState.value = result.getOrElse { error ->
                _uiState.value.copy(
                    isLoading = false,
                    error = error.message ?: "图书馆数据加载失败",
                )
            }
        }
    }

    /** `"3 / 20"` → (3, 20)。格式变了就返回 null，让上层退回 stats 的数据。 */
    private fun parseQuota(text: String?): Pair<Int?, Int?> {
        if (text.isNullOrBlank()) return null to null
        val numbers = Regex("\\d+").findAll(text).map { it.value.toIntOrNull() ?: 0 }.toList()
        return numbers.getOrNull(0) to numbers.getOrNull(1)
    }
}
