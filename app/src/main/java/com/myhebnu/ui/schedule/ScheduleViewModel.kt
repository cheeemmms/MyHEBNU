package com.myhebnu.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myhebnu.data.local.db.entity.CourseEntity
import com.myhebnu.data.local.preferences.UserPreferences
import com.myhebnu.data.repository.ScheduleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class ScheduleUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isCached: Boolean = false,
    val courses: List<CourseEntity> = emptyList(),
    val currentWeek: Int = 1,
    val displayWeek: Int = 1,
    val semesterYear: String = "2025",
    val semesterTerm: String = "12",
    val error: String? = null,
    // Current active course (highlighted)
    val activeCourseId: String? = null,
    // Day labels
    val dayLabels: List<String> = listOf("一", "二", "三", "四", "五", "六", "日"),
    // Period labels and time ranges
    val periodLabels: List<PeriodInfo> = emptyList(),
    // Total number of periods shown
    val maxPeriods: Int = 13
)

data class PeriodInfo(
    val label: String,       // "1-2", "3-4", etc.
    val startPeriod: Int,    // 1
    val endPeriod: Int,      // 2
    val timeRange: String    // "08:00-09:40"
)

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val repository: ScheduleRepository,
    private val preferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    private val today: LocalDate = LocalDate.now()
    private val currentTime: LocalTime = LocalTime.now()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Load preferences
            val year = preferences.currentSemesterYear.first()
            val term = preferences.currentSemesterTerm.first()
            val savedWeek = preferences.currentWeek.first()

            _uiState.update {
                it.copy(
                    semesterYear = year,
                    semesterTerm = term,
                    currentWeek = savedWeek,
                    displayWeek = savedWeek,
                    periodLabels = buildDefaultPeriodLabels()
                )
            }

            // First, check cache
            val cached = repository.hasCachedData(year, term)
            if (cached) {
                _uiState.update { it.copy(isCached = true) }
                // Observe cached data
                repository.observeSchedule(year, term).collect { courses ->
                    _uiState.update {
                        it.copy(
                            courses = courses,
                            isLoading = false,
                            activeCourseId = findActiveCourse(courses, savedWeek)
                        )
                    }
                }
            }

            // Refresh from network
            refreshSchedule()
        }
    }

    fun refreshSchedule() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            val state = _uiState.value
            val result = repository.refreshSchedule(state.semesterYear, state.semesterTerm)
            result.fold(
                onSuccess = {
                    // Room Flow will automatically emit updated data
                    _uiState.update { it.copy(isRefreshing = false, isCached = true) }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            error = if (it.isCached) {
                                null // Don't show error if we have cached data
                            } else {
                                e.message ?: "Failed to load schedule"
                            }
                        )
                    }
                }
            )
        }
    }

    fun goToPreviousWeek() {
        val current = _uiState.value.displayWeek
        if (current > 1) {
            setDisplayWeek(current - 1)
        }
    }

    fun goToNextWeek() {
        val current = _uiState.value.displayWeek
        if (current < 20) {
            setDisplayWeek(current + 1)
        }
    }

    fun goToCurrentWeek() {
        setDisplayWeek(_uiState.value.currentWeek)
    }

    private fun setDisplayWeek(week: Int) {
        _uiState.update {
            it.copy(
                displayWeek = week,
                activeCourseId = findActiveCourse(it.courses, week)
            )
        }
    }

    /**
     * Find the course that is currently active based on day of week and time.
     */
    private fun findActiveCourse(courses: List<CourseEntity>, displayWeek: Int): String? {
        val state = _uiState.value
        if (displayWeek != state.currentWeek) return null

        val todayDayOfWeek = today.dayOfWeek.value // Mon=1 ... Sun=7

        for (course in courses) {
            if (course.dayOfWeek != todayDayOfWeek) continue
            if (displayWeek !in course.startWeek..course.endWeek) continue

            // Find the period info for this course's time range
            val periodInfo = state.periodLabels.find {
                it.startPeriod <= course.startPeriod && it.endPeriod >= course.endPeriod
                    || it.startPeriod <= course.startPeriod && it.endPeriod >= course.startPeriod
            }
            if (periodInfo != null) {
                // Parse the time range
                val times = periodInfo.timeRange.split("-")
                if (times.size == 2) {
                    try {
                        val startTime = LocalTime.parse(times[0], DateTimeFormatter.ofPattern("HH:mm"))
                        val endTime = LocalTime.parse(times[1], DateTimeFormatter.ofPattern("HH:mm"))
                        if (currentTime >= startTime && currentTime <= endTime) {
                            return course.id
                        }
                    } catch (_: Exception) {}
                }
            }
        }
        return null
    }

    /**
     * Build the default period structure based on the教务系统's schedule.
     * Standard Chinese university periods:
     * 1-2 (08:00-09:40), 3-4 (10:00-11:40), 5-6 (14:00-15:40),
     * 7-8 (16:00-17:40), 9-10 (19:00-20:40), 11-13 evening
     */
    private fun buildDefaultPeriodLabels(): List<PeriodInfo> {
        return listOf(
            PeriodInfo("1-2", 1, 2, "08:00-09:40"),
            PeriodInfo("3-4", 3, 4, "10:00-11:40"),
            PeriodInfo("5-6", 5, 6, "14:00-15:40"),
            PeriodInfo("7-8", 7, 8, "16:00-17:40"),
            PeriodInfo("9-10", 9, 10, "19:00-20:40"),
            PeriodInfo("11-13", 11, 13, "20:50-23:00")
        )
    }
}
