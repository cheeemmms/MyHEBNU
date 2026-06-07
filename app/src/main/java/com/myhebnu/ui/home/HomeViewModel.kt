package com.myhebnu.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myhebnu.data.local.preferences.UserPreferences
import com.myhebnu.data.repository.ExamRepository
import com.myhebnu.data.repository.GradeRepository
import com.myhebnu.data.repository.ScheduleRepository
import com.myhebnu.domain.Exam
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

enum class NextClassState { HAS_CLASS, ALL_DONE, WEEKEND, HOLIDAY }

data class HomeUiState(
    val studentName: String = "",
    val greeting: String = "",
    val nextClassState: NextClassState = NextClassState.WEEKEND,
    val nextClassCourse: String = "",
    val nextClassRoom: String = "",
    val nextClassTeacher: String = "",
    val nextClassTime: String = "",
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
    private val gradeRepository: GradeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
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

            val classInfo = computeNextClass(year, term, currentWeek, todayDayOfWeek)
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
        val room: String, val teacher: String, val time: String
    )

    private suspend fun computeNextClass(
        year: String, term: String, week: Int, dayOfWeek: Int
    ): ClassInfo {
        if (dayOfWeek >= 6) return ClassInfo(NextClassState.WEEKEND, "", "", "", "")

        val courses = scheduleRepository.observeSchedule(year, term).first()
        val isOdd = week % 2 == 1
        val todayCourses = courses.filter { c ->
            c.dayOfWeek == dayOfWeek && week in c.startWeek..c.endWeek &&
            when (c.oddEven) { 1 -> isOdd; 2 -> !isOdd; else -> true }
        }.sortedBy { it.startPeriod }

        if (todayCourses.isEmpty()) {
            val hasAnyNearby = courses.any { week in it.startWeek..it.endWeek }
            return if (!hasAnyNearby) ClassInfo(NextClassState.HOLIDAY, "", "", "", "")
            else ClassInfo(NextClassState.ALL_DONE, "", "", "", "")
        }

        val now = LocalTime.now()
        val currentPeriod = estimateCurrentPeriod(now)
        val upcoming = todayCourses.firstOrNull { it.endPeriod >= (currentPeriod ?: 0) }

        return if (upcoming != null) {
            ClassInfo(
                NextClassState.HAS_CLASS, upcoming.courseName,
                upcoming.classroom, upcoming.teacher, "${upcoming.startPeriod}-${upcoming.endPeriod}节"
            )
        } else {
            ClassInfo(NextClassState.ALL_DONE, "", "", "", "")
        }
    }

    private fun estimateCurrentPeriod(time: LocalTime): Int? = when {
        time.hour < 8 -> 0
        time.hour < 10 -> 1
        time.hour < 12 -> 3
        time.hour < 14 -> 4
        time.hour < 16 -> 5
        time.hour < 18 -> 7
        time.hour < 19 -> 8
        time.hour < 21 -> 9
        else -> 11
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
                val all = semesterMap.values.flatten()
                if (all.isEmpty()) return@fold GradeInfo(null, false)
                val totalW = all.sumOf { (it.scoreValue?.toDouble() ?: 0.0) * it.credit.toDouble() }
                val totalC = all.sumOf { it.credit.toDouble() }
                if (totalC > 0) GradeInfo((totalW / totalC).toFloat(), true)
                else GradeInfo(null, false)
            },
            onFailure = { GradeInfo(null, false) }
        )
    }
}
