package com.myhebnu.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * 组件纯逻辑单测。
 *
 * 覆盖三处曾经出错、且无法靠"看截图"发现的判定：
 * 1. `nextCourseIndex` 用 endTime 判定（课前不再误把第二节当成"正在上课"）；
 * 2. 硬滑动 `remainingCourses`（已上完的课必须被丢弃）；
 * 3. `effectiveWeek` 的开学日现算与假期（week=0）语义。
 */
class WidgetScheduleLogicTest {

    private val today = LocalDate.of(2026, 3, 2) // Monday

    private fun course(
        name: String,
        start: String? = "08:00",
        end: String? = "08:40",
        startPeriod: Int = 1,
        endPeriod: Int = 1,
        day: Int = 1,
        hue: Int = 200,
    ) = WidgetCourse(
        courseName = name,
        teacher = "老师",
        classroom = "A101",
        startPeriod = startPeriod,
        endPeriod = endPeriod,
        dayOfWeek = day,
        startTime = start,
        endTime = end,
        colorHue = hue,
        category = "必修"
    )

    // ── nextCourseIndex ──

    @Test
    fun `空课表返回 -1`() {
        assertEquals(-1, WidgetScheduleLogic.nextCourseIndex(emptyList(), LocalTime.of(9, 0)))
    }

    @Test
    fun `第一节课开始前指向第一节`() {
        val courses = listOf(course("高数"), course("英语", start = "09:45", end = "10:25"))
        assertEquals(0, WidgetScheduleLogic.nextCourseIndex(courses, LocalTime.of(7, 30)))
    }

    @Test
    fun `第一节课进行中仍指向第一节`() {
        val courses = listOf(course("高数"), course("英语", start = "09:45", end = "10:25"))
        assertEquals(0, WidgetScheduleLogic.nextCourseIndex(courses, LocalTime.of(8, 30)))
    }

    @Test
    fun `第一节课结束后指向第二节`() {
        val courses = listOf(course("高数"), course("英语", start = "09:45", end = "10:25"))
        assertEquals(1, WidgetScheduleLogic.nextCourseIndex(courses, LocalTime.of(9, 0)))
    }

    @Test
    fun `全部上完返回 -1`() {
        val courses = listOf(course("高数"), course("英语", start = "09:45", end = "10:25"))
        assertEquals(-1, WidgetScheduleLogic.nextCourseIndex(courses, LocalTime.of(12, 0)))
    }

    @Test
    fun `缺少结束时间的课被忽略`() {
        val courses = listOf(
            course("异常数据", end = null),
            course("英语", start = "09:45", end = "10:25")
        )
        assertEquals(1, WidgetScheduleLogic.nextCourseIndex(courses, LocalTime.of(8, 0)))
    }

    // ── isInProgress ──

    @Test
    fun `课中为 true 课前课后为 false`() {
        val c = course("高数")
        assertTrue(WidgetScheduleLogic.isInProgress(c, LocalTime.of(8, 0)))
        assertTrue(WidgetScheduleLogic.isInProgress(c, LocalTime.of(8, 39)))
        assertFalse(WidgetScheduleLogic.isInProgress(c, LocalTime.of(7, 59)))
        assertFalse(WidgetScheduleLogic.isInProgress(c, LocalTime.of(8, 40)))
    }

    @Test
    fun `缺少时间信息时不做判断`() {
        assertFalse(WidgetScheduleLogic.isInProgress(course("x", start = null), LocalTime.of(8, 0)))
        assertFalse(WidgetScheduleLogic.isInProgress(course("x", end = null), LocalTime.of(8, 0)))
    }

    // ── remainingCourses（硬滑动）──

    @Test
    fun `硬滑动丢弃已结束的课`() {
        val courses = listOf(course("A"), course("B"), course("C"))
        val remaining = WidgetScheduleLogic.remainingCourses(courses, nextIndex = 2)
        assertEquals(1, remaining.size)
        assertEquals("C", remaining.first().courseName)
    }

    @Test
    fun `nextIndex 为 0 时保留全部`() {
        val courses = listOf(course("A"), course("B"))
        assertEquals(2, WidgetScheduleLogic.remainingCourses(courses, 0).size)
    }

    @Test
    fun `nextIndex 为 -1 时回落到全量`() {
        val courses = listOf(course("A"), course("B"))
        assertEquals(2, WidgetScheduleLogic.remainingCourses(courses, -1).size)
    }

    // ── microLabel ──

    @Test
    fun `微型组件标签跟随真实上课状态`() {
        assertEquals("正在上课", WidgetScheduleLogic.microLabel(0, inProgress = true, isTomorrow = false))
        assertEquals("下节课", WidgetScheduleLogic.microLabel(0, inProgress = false, isTomorrow = false))
        assertEquals("下节课", WidgetScheduleLogic.microLabel(3, inProgress = false, isTomorrow = false))
        assertEquals("明天", WidgetScheduleLogic.microLabel(0, inProgress = false, isTomorrow = true))
        assertEquals("", WidgetScheduleLogic.microLabel(-1, inProgress = false, isTomorrow = false))
    }

    // ── effectiveWeek ──

    @Test
    fun `无开学日时回落到持久化周次`() {
        assertEquals(7, WidgetScheduleLogic.effectiveWeek(null, null, storedWeek = 7, today = today))
    }

    @Test
    fun `开学前判定为假期`() {
        assertEquals(0, WidgetScheduleLogic.effectiveWeek(today.plusDays(3), null, 5, today))
    }

    @Test
    fun `开学当天为第一周`() {
        assertEquals(1, WidgetScheduleLogic.effectiveWeek(today, null, 5, today))
    }

    @Test
    fun `每满七天进入下一周`() {
        assertEquals(2, WidgetScheduleLogic.effectiveWeek(today, null, 5, today.plusDays(7)))
        assertEquals(3, WidgetScheduleLogic.effectiveWeek(today, null, 5, today.plusDays(14)))
    }

    @Test
    fun `学期结束后判定为假期`() {
        val start = today.minusDays(7 * 15)
        val end = today.minusDays(1)
        assertEquals(0, WidgetScheduleLogic.effectiveWeek(start, end, storedWeek = 16, today = today))
    }

    @Test
    fun `假期周次常量为 0`() {
        assertTrue(WidgetScheduleLogic.isVacation(0))
        assertFalse(WidgetScheduleLogic.isVacation(1))
        assertFalse(WidgetScheduleLogic.isVacation(-1))
    }

    // ── 尺寸自适应 ──

    @Test
    fun `横条组件默认两行放大后最多四行`() {
        assertEquals(2, WidgetScheduleLogic.mediumRowsFor(110f))
        assertEquals(3, WidgetScheduleLogic.mediumRowsFor(210f))
        assertEquals(4, WidgetScheduleLogic.mediumRowsFor(330f))
        assertEquals(4, WidgetScheduleLogic.mediumRowsFor(600f))
    }

    @Test
    fun `列表组件默认四行放大后最多六行`() {
        assertEquals(4, WidgetScheduleLogic.largeRowsFor(280f))
        assertEquals(5, WidgetScheduleLogic.largeRowsFor(350f))
        assertEquals(6, WidgetScheduleLogic.largeRowsFor(440f))
    }

    // ── 刷新节拍 ──

    private val tickBoundaries = listOf(
        LocalTime.of(8, 0), LocalTime.of(8, 40),
        LocalTime.of(9, 45), LocalTime.of(10, 25)
    )

    @Test
    fun `刷新节拍对齐到下一个节次边界`() {
        assertEquals(30L, WidgetScheduleLogic.nextDelayMinutes(LocalDateTime.of(2026, 3, 2, 7, 30), tickBoundaries))
        assertEquals(10L, WidgetScheduleLogic.nextDelayMinutes(LocalDateTime.of(2026, 3, 2, 8, 30), tickBoundaries))
        assertEquals(45L, WidgetScheduleLogic.nextDelayMinutes(LocalDateTime.of(2026, 3, 2, 9, 0), tickBoundaries))
    }

    @Test
    fun `当天边界用完后顺延到次日零点`() {
        assertEquals(180L, WidgetScheduleLogic.nextDelayMinutes(LocalDateTime.of(2026, 3, 2, 21, 0), tickBoundaries))
    }

    @Test
    fun `没有节次数据时最多六小时醒一次`() {
        assertEquals(360L, WidgetScheduleLogic.nextDelayMinutes(LocalDateTime.of(2026, 3, 2, 12, 0), emptyList()))
    }

    @Test
    fun `边界就在眼前时不会排成 0 延迟`() {
        assertEquals(1L, WidgetScheduleLogic.nextDelayMinutes(LocalDateTime.of(2026, 3, 2, 7, 59, 30), tickBoundaries))
    }
}
