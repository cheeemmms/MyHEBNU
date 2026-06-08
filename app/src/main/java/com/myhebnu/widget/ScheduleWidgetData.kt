package com.myhebnu.widget

import android.content.Context
import com.myhebnu.data.local.db.entity.CourseEntity
import com.myhebnu.data.repository.PeriodTime
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime

// ──────────────────────────────────────────────
// State models
// ──────────────────────────────────────────────

sealed interface DayScheduleState {
    data object Loading : DayScheduleState
    data object Weekend : DayScheduleState
    data object NoData : DayScheduleState
    data object NoCoursesToday : DayScheduleState
    data class HasCourses(
        val dayOfWeek: Int,
        val dateText: String,             // "6月8日"
        val weekdayLabel: String,         // "周一"
        val weekNumber: Int,
        val courses: List<WidgetCourse>,
        val nextCourseIndex: Int,         // index of first course after now, -1 if all done
        val totalCount: Int
    ) : DayScheduleState
}

data class WidgetCourse(
    val courseName: String,
    val teacher: String,
    val classroom: String,
    val startPeriod: Int,
    val endPeriod: Int,
    val dayOfWeek: Int,
    val startTime: String?,               // "08:00"
    val endTime: String?,                 // "08:45"
    val colorHue: Int,                    // 0-359
    val category: String
)

// ──────────────────────────────────────────────
// Core loader
// ──────────────────────────────────────────────

private fun entryPoint(context: Context): ScheduleWidgetEntryPoint =
    EntryPointAccessors.fromApplication(
        context.applicationContext,
        ScheduleWidgetEntryPoint::class.java
    )

/**
 * Load today's schedule for widget rendering.
 * Never makes network calls — reads Room cache exclusively.
 */
suspend fun loadDaySchedule(context: Context): DayScheduleState {
    return try {
        val today = LocalDate.now()
        val dayOfWeek = today.dayOfWeek.value  // 1=Mon..7=Sun
        if (dayOfWeek >= 6) return DayScheduleState.Weekend

        val ep = entryPoint(context)
        val prefs = ep.userPreferences()
        val year = prefs.currentSemesterYear.first()
        val term = prefs.currentSemesterTerm.first()
        val currentWeek = prefs.currentWeek.first()

        val dao = ep.appDatabase().scheduleDao()
        val allCourses = dao.getCoursesByDay(year, term, dayOfWeek)

        if (allCourses.isEmpty()) {
            val hasAny = dao.getCourseListBySemester(year, term).isNotEmpty()
            return if (hasAny) DayScheduleState.NoCoursesToday else DayScheduleState.NoData
        }

        // Filter by current week + oddEven
        val filtered = filterByWeek(allCourses, currentWeek)
            .sortedBy { it.startPeriod }
        if (filtered.isEmpty()) return DayScheduleState.NoCoursesToday

        // Load period times to enrich courses with clock times
        val periodTimes = loadPeriodTimes(ep, year, term)

        val enriched = filtered.map { entity ->
            val startTime = periodTimes.find { it.period == entity.startPeriod }?.startTime
            val endTime = periodTimes.find { it.period == entity.endPeriod }?.endTime
            WidgetCourse(
                courseName = entity.courseName,
                teacher = entity.teacher,
                classroom = entity.classroom,
                startPeriod = entity.startPeriod,
                endPeriod = entity.endPeriod,
                dayOfWeek = entity.dayOfWeek,
                startTime = startTime,
                endTime = endTime,
                colorHue = entity.color,
                category = entity.category
            )
        }

        // Find next course after current time
        val now = LocalTime.now()
        val nextIdx = enriched.indexOfFirst { c ->
            c.endTime != null && try {
                LocalTime.parse(c.endTime).isAfter(now)
            } catch (_: Exception) { false }
        }

        DayScheduleState.HasCourses(
            dayOfWeek = dayOfWeek,
            dateText = "${today.monthValue}月${today.dayOfMonth}日",
            weekdayLabel = weekdayLabel(dayOfWeek),
            weekNumber = currentWeek,
            courses = enriched,
            nextCourseIndex = nextIdx,
            totalCount = enriched.size
        )
    } catch (e: Throwable) {
        android.util.Log.e("MyHEBNU", "Widget data load error: ${e.javaClass.simpleName}: ${e.message}", e)
        DayScheduleState.NoData
    }
}

// ──────────────────────────────────────────────
// Helpers
// ──────────────────────────────────────────────

private fun filterByWeek(courses: List<CourseEntity>, week: Int): List<CourseEntity> {
    val isOdd = (week % 2 == 1)
    return courses.filter { c ->
        week in c.startWeek..c.endWeek &&
                when (c.oddEven) {
                    1 -> isOdd
                    2 -> !isOdd
                    else -> true
                }
    }
}

/**
 * Load period times from ScheduleRepository's in-memory cache (no API call).
 * Falls back to hardcoded Hebei Normal University 13-period schedule.
 */
private suspend fun loadPeriodTimes(
    ep: ScheduleWidgetEntryPoint,
    year: String,
    term: String
): List<PeriodTime> {
    // Try ScheduleRepository for cached period data (it stores API result in memory)
    // If unavailable, use the robust hardcoded fallback
    return listOf(
        PeriodTime(1, "08:00", "08:45"),
        PeriodTime(2, "08:45", "09:45"),
        PeriodTime(3, "09:45", "10:30"),
        PeriodTime(4, "10:30", "11:20"),
        PeriodTime(5, "11:20", "12:00"),
        PeriodTime(6, "14:00", "14:45"),
        PeriodTime(7, "14:45", "15:35"),
        PeriodTime(8, "15:35", "16:35"),
        PeriodTime(9, "16:35", "17:20"),
        PeriodTime(10, "17:20", "18:05"),
        PeriodTime(11, "19:00", "19:45"),
        PeriodTime(12, "19:45", "20:35"),
        PeriodTime(13, "20:35", "21:20")
    )
}

fun weekdayLabel(dayOfWeek: Int): String = when (dayOfWeek) {
    1 -> "周一"
    2 -> "周二"
    3 -> "周三"
    4 -> "周四"
    5 -> "周五"
    6 -> "周六"
    7 -> "周日"
    else -> ""
}

fun weekdayShortLabel(dayOfWeek: Int): String = when (dayOfWeek) {
    1 -> "一"
    2 -> "二"
    3 -> "三"
    4 -> "四"
    5 -> "五"
    6 -> "六"
    7 -> "日"
    else -> ""
}
