package com.myhebnu.ui.exam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myhebnu.data.local.preferences.UserPreferences
import com.myhebnu.data.repository.ExamRepository
import com.myhebnu.domain.Exam
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExamUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val exams: List<Exam> = emptyList(),
    val selectedYear: String = "",
    val selectedTerm: String = ""
)

@HiltViewModel
class ExamViewModel @Inject constructor(
    private val repository: ExamRepository,
    private val preferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExamUiState())
    val uiState: StateFlow<ExamUiState> = _uiState.asStateFlow()

    fun loadExams() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // 只查询设置中所设定的当前学期（与首页「下一场考试」、课表、成绩范围一致），
            // 不再使用写死的默认学期，避免陈旧学期的考试长期滞留。
            val year = preferences.currentSemesterYear.first()
            val term = preferences.currentSemesterTerm.first()

            val result = repository.getExams(
                year = year,
                term = term
            )

            result.fold(
                onSuccess = { exams ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = null,
                            exams = exams,
                            selectedYear = year,
                            selectedTerm = term
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "加载考试安排失败",
                            selectedYear = year,
                            selectedTerm = term
                        )
                    }
                }
            )
        }
    }
}
