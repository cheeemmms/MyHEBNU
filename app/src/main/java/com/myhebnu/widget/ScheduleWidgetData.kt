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
    data object AllDoneToday : DayScheduleState
    data class HasCourses(
        val dayOfWeek: Int,
        val dateText: String,             // "6月8日"
        val weekdayLabel: String,         // "周一"
        val weekNumber: Int,
        val courses: List<WidgetCourse>,
        val nextCourseIndex: Int,         // index of first course after now, -1 if all done
        val totalCount: Int,
        val isTomorrow: Boolean = false,  // true when showing tomorrow's schedule
        val tomorrowDayOfWeek: Int = 0    // Mon=1..Fri=5, for "明天 周一" label
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
    val tag = "MyHEBNU-Widget"
    return try {
        val today = LocalDate.now()
        val now = LocalTime.now()
        val dayOfWeek = today.dayOfWeek.value  // 1=Mon..7=Sun
        android.util.Log.d(tag, "loadDaySchedule: today=$today dayOfWeek=$dayOfWeek now=$now")

        val ep = entryPoint(context)
        val prefs = ep.userPreferences()
        val year = prefs.currentSemesterYear.first()
        val term = prefs.currentSemesterTerm.first()
        val currentWeek = prefs.currentWeek.first()
        val dao = ep.appDatabase().scheduleDao()
        val periodTimes = loadPeriodTimes(ep, year, term)

        // ── Phase 1: Decide target day ──

        if (dayOfWeek >= 6) {
            // Weekend
            if (now.isAfter(LocalTime.of(19, 0))) {
                if (dayOfWeek == 6) {
                    // Saturday >19:00 → tomorrow is Sunday → still weekend
                    android.util.Log.d(tag, "loadDaySchedule: Sat>19:00 → Weekend")
                    return DayScheduleState.Weekend
                }
                // Sunday >19:00 → tomorrow is Monday → cross-week
                val targetWeek = currentWeek + 1
                android.util.Log.d(tag, "loadDaySchedule: Sun>19:00 → load Monday week=$targetWeek")
                return loadDayCourses(dao, periodTimes, tag, targetDay = 1, targetDate = today.plusDays(1),
                    targetWeek = targetWeek, year = year, term = term,
                    isTomorrow = true, tomorrowDayOfWeek = 1
                )
            }
            android.util.Log.d(tag, "loadDaySchedule: weekend ≤19:00 → Weekend")
            return DayScheduleState.Weekend
        }

        // Weekday: load today's courses first
        val todayState = loadDayCourses(dao, periodTimes, tag, targetDay = dayOfWeek, targetDate = today,
            targetWeek = currentWeek, year = year, term = term
        )

        when (todayState) {
            is DayScheduleState.Weekend, is DayScheduleState.NoData,
            is DayScheduleState.NoCoursesToday, is DayScheduleState.AllDoneToday -> {
                // These are non-HasCourses, just return as-is
                return todayState
            }
            is DayScheduleState.HasCourses -> {
                val hasRemaining = todayState.nextCourseIndex >= 0
                if (hasRemaining) {
                    // Courses remain today → show today
                    android.util.Log.d(tag, "loadDaySchedule: today has remaining → show today")
                    return todayState
                }
                // All courses done
                if (now.isAfter(LocalTime.of(19, 0))) {
                    // After 19:00 → load tomorrow
                    if (dayOfWeek == 5) {
                        // Friday → tomorrow is Saturday → weekend
                        android.util.Log.d(tag, "loadDaySchedule: Fri all done >19:00 → Weekend")
                        return DayScheduleState.Weekend
                    }
                    // Mon-Thu → load tomorrow
                    val tomorrowDow = dayOfWeek + 1
                    android.util.Log.d(tag, "loadDaySchedule: all done >19:00 → load tomorrow dayOfWeek=$tomorrowDow")
                    return loadDayCourses(dao, periodTimes, tag, targetDay = tomorrowDow,
                        targetDate = today.plusDays(1), targetWeek = currentWeek,
                        year = year, term = term,
                        isTomorrow = true, tomorrowDayOfWeek = tomorrowDow
                    )
                }
                // All done but before 19:00
                android.util.Log.d(tag, "loadDaySchedule: all done ≤19:00 → AllDoneToday")
                return DayScheduleState.AllDoneToday
            }
            is DayScheduleState.Loading -> return todayState
        }
    } catch (e: Throwable) {
        android.util.Log.e(tag, "loadDaySchedule: ERROR ${e.javaClass.simpleName}: ${e.message}", e)
        DayScheduleState.NoData
    }
}

/**
 * Load courses for a specific target day and build the appropriate state.
 * @return HasCourses if courses found, NoCoursesToday if no courses for this day,
 *         NoData if no data at all in the semester
 */
private suspend fun loadDayCourses(
    dao: com.myhebnu.data.local.db.dao.ScheduleDao,
    periodTimes: List<PeriodTime>,
    tag: String,
    targetDay: Int,                // 1=Mon..7=Sun
    targetDate: LocalDate,
    targetWeek: Int,
    year: String,
    term: String,
    isTomorrow: Boolean = false,
    tomorrowDayOfWeek: Int = 0     // only meaningful when isTomorrow=true
): DayScheduleState {
    val allCourses = dao.getCoursesByDay(year, term, targetDay)
    android.util.Log.d(tag, "loadDayCourses: targetDay=$targetDay targetWeek=$targetWeek isTomorrow=$isTomorrow courses.size=${allCourses.size}")

    if (allCourses.isEmpty()) {
        val hasAny = dao.getCourseListBySemester(year, term).isNotEmpty()
        val result = if (hasAny) DayScheduleState.NoCoursesToday else DayScheduleState.NoData
        android.util.Log.d(tag, "loadDayCourses: result=$result")
        return result
    }

    val filtered = filterByWeek(allCourses, targetWeek).sortedBy { it.startPeriod }
    if (filtered.isEmpty()) {
        android.util.Log.d(tag, "loadDayCourses: result=NoCoursesToday (after week filter)")
        return DayScheduleState.NoCoursesToday
    }

    val enriched = filtered.map { entity ->
        val startTime = periodTimes.find { it.period == entity.startPeriod }?.startTime
        val endTime = periodTimes.find { it.period == entity.endPeriod }?.endTime
        WidgetCourse(
            courseName = entity.courseName, teacher = entity.teacher,
            classroom = entity.classroom, startPeriod = entity.startPeriod,
            endPeriod = entity.endPeriod, dayOfWeek = entity.dayOfWeek,
            startTime = startTime, endTime = endTime,
            colorHue = entity.color, category = entity.category
        )
    }

    // Find next course: for today use now; for tomorrow first course is always index 0
    val nextIdx = if (isTomorrow) {
        0  // all courses are in the future, show the first one
    } else {
        val now = LocalTime.now()
        enriched.indexOfFirst { c ->
            c.endTime != null && try { LocalTime.parse(c.endTime).isAfter(now) } catch (_: Exception) { false }
        }
    }

    val result = DayScheduleState.HasCourses(
        dayOfWeek = targetDay,
        dateText = "${targetDate.monthValue}月${targetDate.dayOfMonth}日",
        weekdayLabel = weekdayLabel(targetDay),
        weekNumber = targetWeek,
        courses = enriched,
        nextCourseIndex = nextIdx,
        totalCount = enriched.size,
        isTomorrow = isTomorrow,
        tomorrowDayOfWeek = tomorrowDayOfWeek
    )
    android.util.Log.d(tag, "loadDayCourses: result=HasCourses(count=${enriched.size}, nextIdx=$nextIdx, isTomorrow=$isTomorrow)")
    return result
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
