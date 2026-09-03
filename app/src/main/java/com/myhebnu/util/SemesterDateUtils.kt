package com.myhebnu.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val MONTH_DAY = DateTimeFormatter.ofPattern("M/d")

/**
 * 基于开学日权威计算当前教学周。
 *
 * @return 第 N 周（≥1）；0 = 假期中（开学前 / 放假日之后）；-1 = 未知（未设置开学日，调用方回落 N2154）。
 */
fun computeCurrentWeek(start: LocalDate?, end: LocalDate?, today: LocalDate): Int {
    if (start == null) return -1
    return when {
        today < start -> 0
        end != null && today > end -> 0
        else -> (ChronoUnit.DAYS.between(start, today).toInt() / 7) + 1
    }
}

/**
 * 课表周视图表头月日标签。
 * 约定：week 1 的周一 = 开学日；其余逐日 +1。
 *
 * @param displayWeek 当前展示周（1-based）
 * @param startDate   开学日（week 1 周一），null 时返回空列表
 * @param columns     列数（5 或 7，由周末列开关决定）
 */
fun buildDayDateLabels(displayWeek: Int, startDate: LocalDate?, columns: Int): List<String> {
    if (startDate == null || columns <= 0) return emptyList()
    val base = startDate.plusDays(((displayWeek - 1) * 7L))
    return List(columns) { i -> base.plusDays(i.toLong()).format(MONTH_DAY) }
}
