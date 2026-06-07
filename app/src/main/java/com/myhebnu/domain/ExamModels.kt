package com.myhebnu.domain

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * A single exam entry parsed from the EA system API response.
 *
 * The [examDate], [startTime], and [endTime] are parsed from the `kssj` field
 * which has the format: "2026-07-15(08:30-10:30)".
 */
data class Exam(
    val courseName: String,
    val examDate: LocalDate,
    val startTime: LocalTime?,
    val endTime: LocalTime?,
    val location: String,
    val campus: String,
    val seatNumber: String,
    val examType: String,
    val department: String,
    val className: String,
    val teacherInfo: String,
    val courseSchedule: String,
    val examMethod: String
) {
    /** Days remaining until the exam. Negative = already passed. */
    val daysRemaining: Long
        get() = ChronoUnit.DAYS.between(LocalDate.now(), examDate)

    companion object {
        private val KSSJ_REGEX =
            Regex("""(\d{4}-\d{2}-\d{2})\((\d{2}:\d{2})-(\d{2}:\d{2})\)""")

        /**
         * Parse the `kssj` field format "2026-07-15(08:30-10:30)" into date and time components.
         * Returns null for each component if parsing fails.
         */
        fun parseExamDateTime(kssj: String): Triple<LocalDate?, LocalTime?, LocalTime?> {
            val match = KSSJ_REGEX.find(kssj)
                ?: return Triple(null, null, null)

            val dateStr = match.groupValues[1]
            val startStr = match.groupValues[2]
            val endStr = match.groupValues[3]

            val date = try {
                LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
            } catch (_: Exception) { null }

            val start = try {
                LocalTime.parse(startStr, DateTimeFormatter.ISO_LOCAL_TIME)
            } catch (_: Exception) { null }

            val end = try {
                LocalTime.parse(endStr, DateTimeFormatter.ISO_LOCAL_TIME)
            } catch (_: Exception) { null }

            return Triple(date, start, end)
        }

        /**
         * Format the date for display, e.g. "7月15日 周二".
         */
        fun formatDate(date: LocalDate): String {
            val dayOfWeek = when (date.dayOfWeek.value) {
                1 -> "周一"
                2 -> "周二"
                3 -> "周三"
                4 -> "周四"
                5 -> "周五"
                6 -> "周六"
                7 -> "周日"
                else -> ""
            }
            return "${date.monthValue}月${date.dayOfMonth}日 $dayOfWeek"
        }

        /**
         * Format the time range for display, e.g. "08:30 - 10:30".
         */
        fun formatTimeRange(start: LocalTime?, end: LocalTime?): String {
            if (start == null && end == null) return ""
            val s = start?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: ""
            val e = end?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: ""
            return if (e.isNotEmpty()) "$s - $e" else s
        }
    }
}
