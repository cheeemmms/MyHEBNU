package com.myhebnu.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myhebnu.data.local.preferences.UserPreferences
import com.myhebnu.data.repository.ScheduleRepository
import com.myhebnu.util.computeCurrentWeek
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class SemesterSetupUiState(
    val year: String = LocalDate.now().year.toString(),
    val term: String = "3",
    val isSaving: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SemesterSetupViewModel @Inject constructor(
    private val preferences: UserPreferences,
    private val scheduleRepository: ScheduleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SemesterSetupUiState())
    val uiState: StateFlow<SemesterSetupUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val storedYear = preferences.currentSemesterYear.first()
            val storedTerm = preferences.currentSemesterTerm.first()
            _uiState.update {
                it.copy(
                    year = storedYear.ifBlank { defaultAcademicYear() },
                    term = storedTerm.ifBlank { defaultTerm() }
                )
            }
        }
    }

    fun setYear(year: String) = _uiState.update { it.copy(year = year, error = null) }
    fun setTerm(term: String) = _uiState.update { it.copy(term = term, error = null) }

    /** 拉取 N2154 周次映射，自动推导开学日（week1 起始）与放假日（末周结束），然后持久化。 */
    fun save(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val state = _uiState.value
            val mappingResult = scheduleRepository.fetchWeekDateMapping(state.year, state.term)
            val mapping = mappingResult.getOrNull()

            if (mapping.isNullOrEmpty()) {
                _uiState.update {
                    it.copy(isSaving = false, error = "无法获取该学年学期数据，请检查网络或选择")
                }
                return@launch
            }

            val startRange = mapping[1]
            val lastWeek = mapping.keys.maxOrNull() ?: 1
            val endRange = mapping[lastWeek]
            if (startRange == null || endRange == null) {
                _uiState.update {
                    it.copy(isSaving = false, error = "周次数据不完整，请重试")
                }
                return@launch
            }

            val (startDate, _) = parseDateRange(startRange)
            val (_, endDate) = parseDateRange(endRange)

            preferences.setCurrentSemester(state.year, state.term)
            preferences.setSemesterStartDate(startDate.toString())
            preferences.setSemesterEndDate(endDate.toString())
            preferences.setSemesterManuallySet(true)
            val currentWeek = computeCurrentWeek(startDate, endDate, LocalDate.now())
            preferences.setCurrentWeek(if (currentWeek >= 0) currentWeek else 1)

            _uiState.update { it.copy(isSaving = false) }
            onSuccess()
        }
    }

    private fun defaultAcademicYear(): String {
        val now = LocalDate.now()
        return (if (now.monthValue >= 9) now.year else now.year - 1).toString()
    }

    private fun defaultTerm(): String {
        val now = LocalDate.now()
        val m = now.monthValue
        val d = now.dayOfMonth
        return when {
            m >= 8 || m == 1 -> "3"        // 第一学期（9 月–1 月）
            m == 7 || (m == 6 && d >= 20) -> "16"  // 小学期
            else -> "12"                   // 第二学期
        }
    }

    private fun parseDateRange(range: String): Pair<LocalDate, LocalDate> {
        val parts = range.split("/")
        return if (parts.size == 2) {
            try {
                LocalDate.parse(parts[0]) to LocalDate.parse(parts[1])
            } catch (_: Exception) {
                LocalDate.now() to LocalDate.now()
            }
        } else {
            LocalDate.now() to LocalDate.now()
        }
    }
}
