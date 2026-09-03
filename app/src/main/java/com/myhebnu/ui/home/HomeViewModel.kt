package com.myhebnu.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myhebnu.data.local.preferences.UserPreferences
import com.myhebnu.data.repository.ExamRepository
import com.myhebnu.data.repository.PeriodTime
import com.myhebnu.data.repository.ScheduleRepository
import com.myhebnu.data.repository.fallbackPeriods
import com.myhebnu.data.repository.periodsFromJson
import com.myhebnu.data.repository.UpdateRepository
import com.myhebnu.domain.Exam
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

enum class NextClassState { IN_CLASS, HAS_CLASS, ALL_DONE, WEEKEND, HOLIDAY }

data class HomeUiState(
    val studentName: String = "",
    val greeting: String = "",
    val nextClassState: NextClassState = NextClassState.WEEKEND,
    val nextClassCourse: String = "",
    val nextClassRoom: String = "",
    val nextClassTeacher: String = "",
    val nextClassTime: String = "",
    val nextClassEndTime: String = "",      // "15:35" — used for countdown display
    val nextClassRemaining: String = "",    // "距下课 23 分钟"
    val nextExamCourse: String = "",
    val nextExamDate: String = "",
    val nextExamLocation: String = "",
    val nextExamSeat: String = "",
    val nextExamDays: Long = 0,
    val hasExam: Boolean = false,
    val weightedAvg: Float? = null,
    val hasGrades: Boolean = false,
    val isLoading: Boolean = true,
    /** Latest available version found by the auto launch check (drives the Home update banner). */
    val availableUpdateVersion: String = ""
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val preferences: UserPreferences,
    private val scheduleRepository: ScheduleRepository,
    private val examRepository: ExamRepository,
    private val updateRepository: UpdateRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
        observeCache()
        observeUpdateBanner()
        checkForUpdateOnStart()
    }

    /**
     * Fire-and-forget update check on app launch.
     * Runs concurrently with [loadHomeData] — never blocks the UI.
     */
    private fun checkForUpdateOnStart() {
        viewModelScope.launch {
            try {
                if (preferences.autoCheckUpdate.first()) {
                    updateRepository.checkForUpdate(isManual = false)
                }
            } catch (_: Exception) {
                // Silently ignore
            }
        }
    }

    /**
     * Load the home screen from LOCAL CACHE only — zero network on app launch.
     * Server pulls happen only when the user ENTERS the corresponding page
     * (schedule / exam / grade), which keeps server load minimal.
     */
    fun loadHomeData() {
        viewModelScope.launch {
            val name = preferences.studentName.first()
            val hour = LocalTime.now().hour
            val greetingWord = when (hour) {
                in 6..11 -> "早上好"
                in 12..13 -> "中午好"
                in 14..17 -> "下午好"
                else -> "晚上好"
            }
            val greeting = if (name.isNotEmpty()) "$greetingWord，$name" else greetingWord

            val year = preferences.currentSemesterYear.first()
            val term = preferences.currentSemesterTerm.first()
            val currentWeek = preferences.currentWeek.first()
            val todayDayOfWeek = LocalDate.now().dayOfWeek.value

            // 本地缓存：节次时间表（#30 已持久化到 DataStore），无网络。
            val periods = periodsFromJson(preferences.periodTimesJson.first()).ifEmpty { fallbackPeriods() }
            val classInfo = computeNextClass(year, term, currentWeek, todayDayOfWeek, periods)
            val examInfo = computeNextExam(year, term)

            // 本学期加权均分来自成绩页写入的本地缓存（用 GpaCalculator 计算，与成绩页一致）。
            val avgSemester = preferences.homeWeightedAvgSemester.first()
            val weightedAvg = if (avgSemester.isNotBlank()) preferences.homeWeightedAvg.first() else null

            _uiState.update {
                it.copy(
                    studentName = name,
                    greeting = greeting,
                    nextClassState = classInfo.state,
                    nextClassCourse = classInfo.course,
                    nextClassRoom = classInfo.room,
                    nextClassTeacher = classInfo.teacher,
                    nextClassTime = classInfo.time,
                    nextClassEndTime = classInfo.endTime,
                    nextClassRemaining = classInfo.remaining,
                    nextExamCourse = examInfo.course,
                    nextExamDate = examInfo.date,
                    nextExamLocation = examInfo.location,
                    nextExamSeat = examInfo.seat,
                    nextExamDays = examInfo.days,
                    hasExam = examInfo.hasExam,
                    weightedAvg = weightedAvg,
                    hasGrades = avgSemester.isNotBlank(),
                    isLoading = false
                )
            }
        }
    }

    private data class ClassInfo(
        val state: NextClassState, val course: String,
        val room: String, val teacher: String, val time: String,
        val endTime: String = "", val remaining: String = ""
    )

    /**
     * Determine the current/next class using real period time data from the教务 system.
     *
     * Algorithm:
     * 1. Filter today's courses (day + week + oddEven)
     * 2. For each course, look up its start/end time from [periods]
     * 3. If now falls within any course → IN_CLASS (with countdown to course end)
     * 4. Otherwise find the next upcoming course → HAS_CLASS
     * 5. No courses today → ALL_DONE, WEEKEND, or HOLIDAY
     */
    private suspend fun computeNextClass(
        year: String, term: String, week: Int, dayOfWeek: Int,
        periods: List<PeriodTime>
    ): ClassInfo {
        if (dayOfWeek >= 6) return ClassInfo(NextClassState.WEEKEND, "", "", "", "")

        val courses = scheduleRepository.observeSchedule(year, term).first()
        val isOdd = week % 2 == 1
        val todayCourses = courses.filter { c ->
            c.dayOfWeek == dayOfWeek && week in c.startWeek..c.endWeek &&
            when (c.oddEven) { 1 -> isOdd; 2 -> !isOdd; else -> true }
        }.sortedBy { it.startPeriod }

        // No courses today
        if (todayCourses.isEmpty()) {
            if (courses.isEmpty()) {
                // Data not loaded yet — don't claim HOLIDAY
                return ClassInfo(NextClassState.ALL_DONE, "", "", "", "")
            }
            val hasAnyNearby = courses.any { week in it.startWeek..it.endWeek }
            return if (!hasAnyNearby) ClassInfo(NextClassState.HOLIDAY, "", "", "", "")
            else ClassInfo(NextClassState.ALL_DONE, "", "", "", "")
        }

        val now = LocalTime.now()

        // ① Check if any course is currently in progress
        for (course in todayCourses) {
            val courseStartTime = periods.firstOrNull { it.period == course.startPeriod }?.startTime
            val courseEndTime = periods.firstOrNull { it.period == course.endPeriod }?.endTime
            if (courseStartTime != null && courseEndTime != null) {
                val start = LocalTime.parse(courseStartTime)
                val end = LocalTime.parse(courseEndTime)
                if (now >= start && now < end) {
                    val remainingMinutes = ChronoUnit.MINUTES.between(now, end)
                    return ClassInfo(
                        NextClassState.IN_CLASS,
                        course.courseName, course.classroom, course.teacher,
                        "${course.startPeriod}-${course.endPeriod}节",
                        courseEndTime,
                        "距下课 ${remainingMinutes}分钟"
                    )
                }
            }
        }

        // ② No course in progress → find next upcoming course
        val upcoming = todayCourses.firstOrNull { course ->
            val courseStartTime =
                periods.firstOrNull { it.period == course.startPeriod }?.startTime
            if (courseStartTime != null) {
                LocalTime.parse(courseStartTime) > now
            } else false
        }

        return if (upcoming != null) {
            ClassInfo(
                NextClassState.HAS_CLASS, upcoming.courseName,
                upcoming.classroom, upcoming.teacher,
                "${upcoming.startPeriod}-${upcoming.endPeriod}节"
            )
        } else {
            ClassInfo(NextClassState.ALL_DONE, "", "", "", "")
        }
    }

    private data class ExamInfo(
        val course: String, val date: String, val location: String,
        val seat: String, val days: Long, val hasExam: Boolean
    )

    /**
     * Next exam is read from the local Room cache (written when the exam page is entered).
     * No network call on the home screen.
     */
    private suspend fun computeNextExam(year: String, term: String): ExamInfo {
        val exams = examRepository.getCachedExams(year, term)
        val next = exams.firstOrNull { it.daysRemaining >= 0 }
        return if (next != null) {
            ExamInfo(
                next.courseName, Exam.formatDate(next.examDate),
                next.location, next.seatNumber, next.daysRemaining, true
            )
        } else ExamInfo("", "", "", "", 0, false)
    }

    /**
     * Keep the home weighted-average card live: re-read the local cache whenever
     * the grades page writes a fresh value (so returning from the grades page updates the card).
     */
    /**
     * Mirror the auto-check result into UI state so the Home screen can render an
     * in-app update banner whenever a newer version is available.
     */
    private fun observeUpdateBanner() {
        viewModelScope.launch {
            preferences.availableUpdateVersion.collect { version ->
                _uiState.update { it.copy(availableUpdateVersion = version) }
            }
        }
    }

    private fun observeCache() {
        viewModelScope.launch {
            combine(preferences.homeWeightedAvg, preferences.homeWeightedAvgSemester) { avg, sem ->
                sem to avg
            }.collect { (sem, avg) ->
                _uiState.update {
                    it.copy(
                        weightedAvg = if (sem.isNotBlank()) avg else null,
                        hasGrades = sem.isNotBlank()
                    )
                }
            }
        }
    }
}
