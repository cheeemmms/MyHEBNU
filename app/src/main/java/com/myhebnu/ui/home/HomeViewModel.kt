package com.myhebnu.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myhebnu.data.local.preferences.UserPreferences
import com.myhebnu.data.repository.ExamRepository
import com.myhebnu.data.repository.GradeRepository
import com.myhebnu.data.repository.PeriodTime
import com.myhebnu.data.repository.ScheduleRepository
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
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val preferences: UserPreferences,
    private val scheduleRepository: ScheduleRepository,
    private val examRepository: ExamRepository,
    private val gradeRepository: GradeRepository,
    private val updateRepository: UpdateRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
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

    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

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

            // Fetch real period times from API (used for accurate current-period detection)
            val periods = scheduleRepository.fetchPeriods(year, term)

            val classInfo = computeNextClass(year, term, currentWeek, todayDayOfWeek, periods)
            val examInfo = computeNextExam(year, term)
            val gradeInfo = computeGradeInfo()

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
                    weightedAvg = gradeInfo.avg,
                    hasGrades = gradeInfo.hasGrades,
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

    private suspend fun computeNextExam(year: String, term: String): ExamInfo {
        val result = examRepository.getExams(year, term)
        return result.fold(
            onSuccess = { exams ->
                val next = exams.firstOrNull { it.daysRemaining >= 0 }
                if (next != null) {
                    ExamInfo(
                        next.courseName, Exam.formatDate(next.examDate),
                        next.location, next.seatNumber, next.daysRemaining, true
                    )
                } else ExamInfo("", "", "", "", 0, false)
            },
            onFailure = { ExamInfo("", "", "", "", 0, false) }
        )
    }

    private data class GradeInfo(val avg: Float?, val hasGrades: Boolean)

    private suspend fun computeGradeInfo(): GradeInfo {
        val result = gradeRepository.getAllGrades()
        return result.fold(
            onSuccess = { semesterMap ->
                if (semesterMap.isEmpty()) return@fold GradeInfo(null, false)
                // 取最新非空学期（key 如 "2025-2026-2" 降序 = 最新在前）
                val newest = semesterMap.entries
                    .sortedByDescending { it.key }
                    .firstOrNull { it.value.isNotEmpty() }
                    ?: return@fold GradeInfo(null, false)
                val grades = newest.value
                val totalW = grades.sumOf { (it.scoreValue?.toDouble() ?: 0.0) * it.credit.toDouble() }
                val totalC = grades.sumOf { it.credit.toDouble() }
                if (totalC > 0) GradeInfo((totalW / totalC).toFloat(), true)
                else GradeInfo(null, false)
            },
            onFailure = { GradeInfo(null, false) }
        )
    }
}
