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
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class ScheduleUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isCached: Boolean = false,
    val courses: List<CourseEntity> = emptyList(),
    val filteredCourses: List<CourseEntity> = emptyList(),  // 按周过滤后的课程
    val currentWeek: Int = 1,
    val displayWeek: Int = 1,
    val semesterYear: String = "2025",
    val semesterTerm: String = "12",
    val error: String? = null,
    // Current active course (highlighted)
    val activeCourseId: String? = null,
    // Day labels
    val dayLabels: List<String> = listOf("一", "二", "三", "四", "五"),
    // Period labels and time ranges
    val periodLabels: List<PeriodInfo> = emptyList(),
    // Course tonal palettes (name → palette)
    val coursePalettes: Map<String, com.myhebnu.ui.theme.CourseTonalPalette> = emptyMap(),
    // Course detail BottomSheet
    val selectedCourse: CourseEntity? = null
)

data class PeriodInfo(
    val label: String,         // "1-2", "3-4", etc.
    val startPeriod: Int,      // 1
    val endPeriod: Int,        // 2
    val startTime: String,     // "08:00"
    val endTime: String,       // "09:40"
    val timeRange: String      // "08:00-09:40" (kept for backward compat)
)

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val repository: ScheduleRepository,
    private val preferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    // Separate Flow to drive combine — avoids potential races when _uiState
    // is updated concurrently from the outer coroutine and the combine collector.
    private val _displayWeek = MutableStateFlow(1)

    private val today: LocalDate = LocalDate.now()
    private val currentTime: LocalTime = LocalTime.now()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // ① 探测/切换学期
            val (year, term) = detectAndApplySemester()

            // ② 从 N2154 API 获取周次日期映射
            val weekMappingResult = repository.fetchWeekDateMapping(year, term)

            // ③ 自动计算当前周
            val today = LocalDate.now()
            val autoWeek = weekMappingResult.fold(
                onSuccess = { mapping ->
                    mapping.entries.find { (_, dateRange) ->
                        val (start, end) = parseDateRange(dateRange)
                        today in start..end
                    }?.key ?: preferences.currentWeek.first()
                },
                onFailure = { preferences.currentWeek.first() }
            )
            preferences.setCurrentWeek(autoWeek)

            // Sync both displayWeek sources
            _displayWeek.value = autoWeek
            _uiState.update {
                it.copy(
                    semesterYear = year, semesterTerm = term,
                    currentWeek = autoWeek, displayWeek = autoWeek,
                    periodLabels = buildPerPeriodLabels()
                )
            }

            // Check cache first
            val cached = repository.hasCachedData(year, term)
            if (cached) {
                _uiState.update { it.copy(isCached = true) }
            }

            // ④ combine: Room 课程 + 独立的 displayWeek Flow → 自动过滤
            // 使用独立的 _displayWeek 而不是 _uiState.map{}，避免
            // StateFlow 合并更新时潜在的竞态导致 combine 错过触发信号
            viewModelScope.launch {
                combine(
                    repository.observeSchedule(year, term),
                    _displayWeek
                ) { allCourses, week ->
                    allCourses to filterCoursesByWeek(allCourses, week)
                }.collect { (allCourses, filtered) ->
                    _uiState.update {
                        it.copy(
                            courses = allCourses,
                            filteredCourses = filtered,
                            coursePalettes = buildCoursePalettes(allCourses),
                            isLoading = false,
                            activeCourseId = findActiveCourse(filtered, it.displayWeek)
                        )
                    }
                }
            }

            // ⑤ 刷新课表 (后台，不阻塞 UI)
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
        // Update the independent Flow FIRST — this triggers combine re-evaluation
        _displayWeek.value = week
        // Then update the UI state (activeCourseId will be overridden by combine result)
        _uiState.update {
            it.copy(
                displayWeek = week,
                activeCourseId = findActiveCourse(it.filteredCourses, week)
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
     * Guess the current semester based on the phone's date.
     *
     * Chinese academic year: fall (Sep) = first semester (xqm=3),
     * spring (Feb) = second semester (xqm=12).
     *   month ∈ [8, 12] → xqm="3",  xnm=currentYear
     *   month ∈ [1, 7]  → xqm="12", xnm=currentYear-1
     */
    private fun guessCurrentSemester(): Pair<String, String> {
        val now = LocalDate.now()
        return when (now.monthValue) {
            in 8..12 -> now.year.toString() to "3"
            in 1..7  -> (now.year - 1).toString() to "12"
            else     -> "2025" to "12"
        }
    }

    /**
     * Detect the current semester. Compares the phone-date guess against
     * the stored semester. If they differ, validates the guess via the
     * N2154 API. Switches only when the new semester is confirmed active.
     */
    private suspend fun detectAndApplySemester(): Pair<String, String> {
        val storedYear = preferences.currentSemesterYear.first()
        val storedTerm = preferences.currentSemesterTerm.first()
        val guessed = guessCurrentSemester()

        // Same → no switch needed
        if (storedYear == guessed.first && storedTerm == guessed.second) {
            return storedYear to storedTerm
        }

        // Different → validate via API
        val result = repository.fetchWeekDateMapping(guessed.first, guessed.second)
        return if (result.isSuccess && result.getOrNull()?.isNotEmpty() == true) {
            preferences.setCurrentSemester(guessed.first, guessed.second)
            android.util.Log.w("MyHEBNU", "Semester switched: $storedYear-$storedTerm → ${guessed.first}-${guessed.second}")
            guessed
        } else {
            android.util.Log.w("MyHEBNU", "Semester guess ${guessed.first}-${guessed.second} invalid (break?), keeping $storedYear-$storedTerm")
            storedYear to storedTerm
        }
    }

    /**
     * Filter courses that are active in the given week.
     * Checks week range AND odd/even week restriction.
     */
    private fun filterCoursesByWeek(
        courses: List<CourseEntity>, week: Int
    ): List<CourseEntity> {
        val isOdd = (week % 2 == 1)
        return courses.filter { course ->
            week in course.startWeek..course.endWeek &&
            when (course.oddEven) {
                1 -> isOdd
                2 -> !isOdd
                else -> true
            }
        }
    }

    /**
     * Parse a date range string like "2025-09-08/2025-09-14" into a pair of LocalDates.
     */
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

    /**
     * Build the default period structure based on the教务系统's schedule.
     * Standard Chinese university periods:
     * 1-2 (08:00-09:40), 3-4 (10:00-11:40), 5-6 (14:00-15:40),
     * 7-8 (16:00-17:40), 9-10 (19:00-20:40), 11-13 evening
     */
    /** Build per-period labels: 1, 2, 3...11 (each period is one row). */
    private fun buildPerPeriodLabels(): List<PeriodInfo> {
        val times = listOf(
            1 to ("08:00" to "08:50"),   2 to ("08:55" to "09:40"),
            3 to ("10:00" to "10:50"),   4 to ("10:55" to "11:40"),
            5 to ("14:00" to "14:50"),   6 to ("14:55" to "15:40"),
            7 to ("16:00" to "16:50"),   8 to ("16:55" to "17:40"),
            9 to ("19:00" to "19:50"),  10 to ("19:55" to "20:40"),
            11 to ("20:50" to "23:00")
        )
        return times.map { (p, t) ->
            PeriodInfo(p.toString(), p, p, t.first, t.second, "${t.first}-${t.second}")
        }
    }

    /** Build tonal palettes for all courses in the current semester. */
    private fun buildCoursePalettes(courses: List<CourseEntity>, isDark: Boolean = false): Map<String, com.myhebnu.ui.theme.CourseTonalPalette> {
        val names = courses.map { it.courseName }.distinct()
        val hues = com.myhebnu.ui.theme.assignCourseHues(names)
        return names.associateWith { name ->
            com.myhebnu.ui.theme.coursePaletteForHue(hues[name] ?: 0f, isDark)
        }
    }

    fun selectCourse(course: CourseEntity?) {
        _uiState.update { it.copy(selectedCourse = course) }
    }
}
