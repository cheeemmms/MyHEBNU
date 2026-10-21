package com.myhebnu.widget

import android.content.Context
import com.myhebnu.data.local.db.entity.CourseEntity
import com.myhebnu.data.repository.PeriodTime
import com.google.gson.JsonParser
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
    data object Vacation : DayScheduleState
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
        val storedWeek = prefs.currentWeek.first()
        val startDate = prefs.semesterStartDate.first().toLocalDateOrNull()
        val endDate = prefs.semesterEndDate.first().toLocalDateOrNull()
        // 周次在渲染时现算，避免"一周没开 App 组件还停在上周"。
        val currentWeek = WidgetScheduleLogic.effectiveWeek(startDate, endDate, storedWeek, today)
        android.util.Log.d(
            tag,
            "loadDaySchedule: storedWeek=$storedWeek week=$currentWeek startDate=$startDate endDate=$endDate"
        )
        val dao = ep.appDatabase().scheduleDao()
        val periodTimes = loadPeriodTimes(ep, year, term)

        // ── Phase 0: 假期（开学前 / 学期结束后）与其它空态区分开 ──
        if (WidgetScheduleLogic.isVacation(currentWeek)) {
            val hasAny = dao.getCourseListBySemester(year, term).isNotEmpty()
            val result = if (hasAny) DayScheduleState.Vacation else DayScheduleState.NoData
            android.util.Log.d(tag, "loadDaySchedule: week=0 → $result")
            return result
        }

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
                    targetWeek = targetWeek, year = year, term = term, now = now,
                    isTomorrow = true, tomorrowDayOfWeek = 1
                )
            }
            android.util.Log.d(tag, "loadDaySchedule: weekend ≤19:00 → Weekend")
            return DayScheduleState.Weekend
        }

        // Weekday: load today's courses first
        val todayState = loadDayCourses(dao, periodTimes, tag, targetDay = dayOfWeek, targetDate = today,
            targetWeek = currentWeek, year = year, term = term, now = now
        )

        when (todayState) {
            is DayScheduleState.Weekend, is DayScheduleState.NoData, is DayScheduleState.Vacation,
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
                        year = year, term = term, now = now,
                        isTomorrow = true, tomorrowDayOfWeek = tomorrowDow
                    )
                }
                // All done but before 19:00
                android.util.Log.d(tag, "loadDaySchedule: all done ≤19:00 → AllDoneToday")
                return DayScheduleState.AllDoneToday
            }
            is DayScheduleState.Loading, is DayScheduleState.Vacation -> return todayState
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
    now: LocalTime,
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
        WidgetScheduleLogic.nextCourseIndex(enriched, now)
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
 * Load period times for widget rendering.
 * Prefers the real timetable persisted by [com.myhebnu.data.repository.ScheduleRepository]
 * (shared cross-process via DataStore); falls back to the hardcoded Hebei Normal
 * University 13-period schedule when no persisted data is available. Fixes #28
 * (widget previously always used the hardcoded table, ignoring the real one).
 */
private suspend fun loadPeriodTimes(
    ep: ScheduleWidgetEntryPoint,
    year: String,
    term: String
): List<PeriodTime> {
    val json = ep.userPreferences().periodTimesJson.first()
    if (json.isNotBlank()) {
        try {
            val arr = JsonParser.parseString(json).asJsonArray
            val list = mutableListOf<PeriodTime>()
            for (item in arr) {
                val o = item.asJsonObject
                list.add(
                    PeriodTime(
                        o.get("period")?.asInt ?: 0,
                        o.get("startTime")?.asString ?: "",
                        o.get("endTime")?.asString ?: ""
                    )
                )
            }
            if (list.isNotEmpty()) return list
        } catch (_: Exception) {
            android.util.Log.w("MyHEBNU-Widget", "loadPeriodTimes: failed to parse persisted JSON, using fallback")
        }
    }
    return hardcodedPeriods()
}

/**
 * 供刷新节拍使用：读取真实节次表（读取失败/为空时回退硬编码课表）。
 * 与组件渲染共用 [loadPeriodTimes]，保证"什么时候该醒"和"醒来后显示什么"用的是同一份时间表。
 */
internal suspend fun loadWidgetPeriodTimes(context: Context): List<PeriodTime> =
    loadPeriodTimes(entryPoint(context), "", "")

/** Hardcoded fallback matching the real 河北师大 13-period schedule (裕华校区). */private fun hardcodedPeriods(): List<PeriodTime> = listOf(
    PeriodTime(1, "08:00", "08:40"),
    PeriodTime(2, "08:50", "09:30"),
    PeriodTime(3, "09:45", "10:25"),
    PeriodTime(4, "10:35", "11:15"),
    PeriodTime(5, "11:20", "12:05"),
    PeriodTime(6, "14:00", "14:40"),
    PeriodTime(7, "14:50", "15:30"),
    PeriodTime(8, "15:35", "16:20"),
    PeriodTime(9, "16:35", "17:15"),
    PeriodTime(10, "17:25", "18:05"),
    PeriodTime(11, "19:00", "19:40"),
    PeriodTime(12, "19:50", "20:30"),
    PeriodTime(13, "20:35", "21:20")
)

private fun String.toLocalDateOrNull(): LocalDate? =
    if (isBlank()) null else runCatching { LocalDate.parse(this) }.getOrNull()

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
