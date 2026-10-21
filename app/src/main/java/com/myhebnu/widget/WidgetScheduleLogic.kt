package com.myhebnu.widget

import com.myhebnu.util.computeCurrentWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/**
 * 课外组件（桌面小组件）的纯逻辑层。
 *
 * 这里只允许出现 java.time / kotlin 标准库，不引用任何 `android.*`，
 * 以便在 `app/src/test` 里用普通 JUnit 直接覆盖，不需要 Robolectric。
 *
 * 三个组件共用同一套判定，避免"微型说正在上课、横条说下节课"这类不一致。
 */
object WidgetScheduleLogic {

    /** 假期周次（与 `computeCurrentWeek` 的 0 约定一致）。 */
    const val VACATION_WEEK: Int = 0

    /** 宽松解析 "08:00"，脏数据返回 null 而不是抛异常。 */
    fun parseTime(raw: String?): LocalTime? {
        if (raw.isNullOrBlank()) return null
        return try {
            LocalTime.parse(raw.trim())
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 第一条"还没上完"的课的下标；全部上完返回 -1。
     *
     * 判定用 endTime 而不是 startTime：8:30 时 8:00-8:40 的课仍算未完成，
     * 这正是旧实现 `indexOfFirst { startTime > now }` 在课前误判的根源之一。
     */
    fun nextCourseIndex(courses: List<WidgetCourse>, now: LocalTime): Int =
        courses.indexOfFirst { course -> parseTime(course.endTime)?.isAfter(now) == true }

    /** [now] 是否落在课程时间窗内（闭开区间：开始含、结束不含）。缺少任一时刻返回 false。 */
    fun isInProgress(course: WidgetCourse, now: LocalTime): Boolean {
        val start = parseTime(course.startTime) ?: return false
        val end = parseTime(course.endTime) ?: return false
        return !now.isBefore(start) && now.isBefore(end)
    }

    /**
     * 硬滑动：只保留"尚未上完"的课，已结束的直接丢弃。
     *
     * 与旧的渐变窗口（始终按列表前 N 条展示）不同，下午 3 点打开的组件
     * 不会再把上午的课重新挤回来。
     */
    fun remainingCourses(courses: List<WidgetCourse>, nextIndex: Int): List<WidgetCourse> =
        if (nextIndex < 0) courses else courses.drop(nextIndex)

    /** 微型组件的状态小标题。 */
    fun microLabel(nextIndex: Int, inProgress: Boolean, isTomorrow: Boolean): String = when {
        isTomorrow -> "明天"
        inProgress -> "正在上课"
        nextIndex >= 0 -> "下节课"
        else -> ""
    }

    /**
     * 渲染时刻现算的教学周。
     *
     * 旧实现直接读持久化的 `current_week`，一周不开 App 就永久停在旧周次；
     * 这里以开学日为权威，未设置开学日（null）时才回落到持久化值。
     */
    fun effectiveWeek(
        startDate: LocalDate?,
        endDate: LocalDate?,
        storedWeek: Int,
        today: LocalDate
    ): Int {
        val computed = computeCurrentWeek(startDate, endDate, today)
        return if (computed == -1) storedWeek else computed
    }

    /** 假期中（开学前 / 学期结束后）。 */
    fun isVacation(week: Int): Boolean = week == VACATION_WEEK

    // ── 尺寸自适应（LocalSize → 行数）──
    //
    // 只把高度换算成"显示几行"的整数，再交给 take()，不把计算结果当 dp 传给布局：
    // MIUI 会把布局里的 Int dp 当资源 ID 解析，这里刻意避开那条路径。

    /** 横条组件：默认 110dp 高显示 2 行，放大后最多 4 行。 */
    fun mediumRowsFor(heightDp: Float): Int = when {
        heightDp >= 330f -> 4
        heightDp >= 210f -> 3
        else -> 2
    }

    /** 列表组件：默认 280dp 高显示 4 行，放大后最多 6 行。 */
    fun largeRowsFor(heightDp: Float): Int = when {
        heightDp >= 440f -> 6
        heightDp >= 350f -> 5
        else -> 4
    }

    // ── 刷新节拍 ──

    /** 下限 1 分钟：避免"边界刚好在现在"导致 0 延迟的忙循环。 */
    const val MIN_TICK_MINUTES: Long = 1L

    /** 上限 6 小时：即使没有节次数据，一天也至少被刷新几次。 */
    const val MAX_TICK_MINUTES: Long = 360L

    /**
     * 距离下一个"组件内容会变化"的时刻还有多少分钟。
     *
     * 边界 = 每个节次的开始/结束时刻 + 零点（跨天后日期、星期、课程都会变）。
     * 当天边界用完就顺延到次日第一个边界；结果夹在 [MIN_TICK_MINUTES, MAX_TICK_MINUTES]。
     */
    fun nextDelayMinutes(now: LocalDateTime, periodBoundaries: List<LocalTime>): Long {
        val boundaries = (periodBoundaries + LocalTime.MIDNIGHT).distinct().sorted()
        val todayTime = now.toLocalTime()
        val nextToday = boundaries.firstOrNull { todayTime.isBefore(it) }
        val target = if (nextToday != null) {
            LocalDateTime.of(now.toLocalDate(), nextToday)
        } else {
            LocalDateTime.of(now.toLocalDate().plusDays(1), boundaries.first())
        }
        val minutes = ChronoUnit.MINUTES.between(now, target)
        return minutes.coerceIn(MIN_TICK_MINUTES, MAX_TICK_MINUTES)
    }
}
