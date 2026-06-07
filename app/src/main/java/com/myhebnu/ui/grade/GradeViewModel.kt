package com.myhebnu.ui.grade

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myhebnu.data.repository.GradeRepository
import com.myhebnu.domain.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GradeUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val warningMessage: String? = null,        // snackbar warning when refresh fails but cache exists
    val semesters: List<SemesterGrades> = emptyList(),
    val currentStrategy: GpaStrategy = GpaStrategy.WEIGHTED_PERCENTAGE,
    val expandedSemester: String? = null,     // semesterName to expand
    val expandedCourse: String? = null,        // classId whose detail is loading/shown
    val courseDetails: Map<String, List<GradeSubItem>> = emptyMap(),
    val loadingDetail: Boolean = false
)

@HiltViewModel
class GradeViewModel @Inject constructor(
    private val repository: GradeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GradeUiState())
    val uiState: StateFlow<GradeUiState> = _uiState.asStateFlow()

    // In-memory cache: survives within process lifetime, prevents data loss on refresh failure
    private var cachedSemesters: List<SemesterGrades>? = null

    fun loadAllGrades() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = repository.getAllGrades()
            result.fold(
                onSuccess = { semesterMap ->
                    val strategy = _uiState.value.currentStrategy
                    val semesters = semesterMap.map { (name, grades) ->
                        val sorted = grades.sortedByDescending { it.scoreValue ?: 0f }
                        SemesterGrades(
                            semesterYear = sorted.firstOrNull()?.semesterYear ?: "",
                            semesterTerm = sorted.firstOrNull()?.semesterTerm ?: "",
                            semesterName = name,
                            courses = sorted,
                            gpa4 = GpaCalculator.calculate(sorted, GpaStrategy.SCALE_4_0),
                            gpa5 = GpaCalculator.calculate(sorted, GpaStrategy.SCALE_5_0),
                            weightedAvg = GpaCalculator.calculate(sorted, GpaStrategy.WEIGHTED_PERCENTAGE)
                        )
                    }.sortedByDescending { it.semesterName }

                    // Persist to in-memory cache before updating UI state
                    cachedSemesters = semesters

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = null,
                            warningMessage = null,
                            semesters = semesters,
                            expandedSemester = semesters.firstOrNull()?.semesterName
                        )
                    }
                },
                onFailure = { e ->
                    val cached = cachedSemesters
                    if (cached != null && cached.isNotEmpty()) {
                        // Refresh failed but we have cached data — keep showing it with a warning
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = null,
                                warningMessage = e.message ?: "刷新失败，显示的是上次的数据"
                            )
                        }
                    } else {
                        // No cache — show error screen with retry
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = e.message ?: "加载成绩失败",
                                warningMessage = null
                            )
                        }
                    }
                }
            )
        }
    }

    fun clearWarning() {
        _uiState.update { it.copy(warningMessage = null) }
    }

    fun toggleSemesterExpanded(semesterName: String) {
        _uiState.update {
            if (it.expandedSemester == semesterName) {
                it.copy(expandedSemester = null)
            } else {
                it.copy(expandedSemester = semesterName)
            }
        }
    }

    fun loadGradeDetail(classId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(loadingDetail = true, expandedCourse = classId) }

            // Find the semester for this course
            val state = _uiState.value
            var year = ""
            var term = ""
            for (sem in state.semesters) {
                val course = sem.courses.find { it.classId == classId }
                if (course != null) {
                    year = course.semesterYear
                    term = course.semesterTerm
                    break
                }
            }

            val result = repository.getGradeDetail(year, term, classId)
            result.fold(
                onSuccess = { details ->
                    _uiState.update {
                        it.copy(
                            loadingDetail = false,
                            courseDetails = it.courseDetails + (classId to details)
                        )
                    }
                },
                onFailure = {
                    _uiState.update { it.copy(loadingDetail = false) }
                }
            )
        }
    }

    fun dismissDetail() {
        _uiState.update { it.copy(expandedCourse = null) }
    }

    fun setStrategy(strategy: GpaStrategy) {
        _uiState.update { it.copy(currentStrategy = strategy) }
    }
}
