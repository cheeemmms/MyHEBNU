package com.myhebnu.ui.exam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myhebnu.data.repository.ExamRepository
import com.myhebnu.domain.Exam
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExamUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val exams: List<Exam> = emptyList(),
    val selectedYear: String = "2025",
    val selectedTerm: String = "12"
)

@HiltViewModel
class ExamViewModel @Inject constructor(
    private val repository: ExamRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExamUiState())
    val uiState: StateFlow<ExamUiState> = _uiState.asStateFlow()

    fun loadExams() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val state = _uiState.value
            val result = repository.getExams(
                year = state.selectedYear,
                term = state.selectedTerm
            )

            result.fold(
                onSuccess = { exams ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = null,
                            exams = exams
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "加载考试安排失败"
                        )
                    }
                }
            )
        }
    }
}
