package com.myhebnu.ui.library

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * 图书馆模块里反复用到的展示层格式化。
 *
 * 汇文系统的时间表示有三套（`yyyy-MM-dd`、`yyyy-MM-dd HH:mm[:ss]`、epoch 毫秒），
 * 金额一律是 `"CNY88.00"` 这类带币种的字符串，所以集中在这里处理，避免每个页面各写一遍。
 */
object LibraryFormat {

    private val DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    /** 解析 `yyyy-MM-dd` 或 `yyyy-MM-dd HH:mm[:ss]` 的日期部分，失败返回 null。 */
    private fun parseDate(text: String?): LocalDate? {
        if (text.isNullOrBlank()) return null
        val head = text.trim().take(10)
        return runCatching { LocalDate.parse(head, DATE) }.getOrNull()
    }

    /** 距离今天还有几天；负数表示已逾期。无法解析时返回 null。 */
    fun daysUntil(text: String?): Long? {
        val date = parseDate(text) ?: return null
        return ChronoUnit.DAYS.between(LocalDate.now(), date)
    }

    /** `2026-11-26` → `11月26日`，让列表少一点数字噪音。 */
    fun shortDate(text: String?): String {
        val date = parseDate(text) ?: return text.orEmpty()
        return "${date.monthValue}月${date.dayOfMonth}日"
    }

    /** `2026-10-27 18:30:29` → `10月27日 18:30`。 */
    fun shortDateTime(text: String?): String {
        if (text.isNullOrBlank()) return ""
        val normalized = text.trim().replace('/', '-')
        return runCatching {
            val cut = if (normalized.length >= 16) normalized.take(16) else normalized
            val dt = java.time.LocalDateTime.parse(cut, DATE_TIME)
            "${dt.monthValue}月${dt.dayOfMonth}日 " +
                "${dt.hour.toString().padStart(2, '0')}:${dt.minute.toString().padStart(2, '0')}"
        }.getOrDefault(text)
    }

    /** `"CNY88.00"` → `88.00`；`"CNY0.00"` → `0.00`。 */
    fun money(text: String?): String {
        if (text.isNullOrBlank()) return "0.00"
        val digits = text.filter { it.isDigit() || it == '.' }
        return digits.ifBlank { "0.00" }
    }

    fun money(value: Double?): String {
        val v = value ?: 0.0
        return if (v == 0.0) "0.00" else String.format("%.2f", v)
    }

    /** 应还日期的语气：逾期 / 临期（7 天内）/ 正常。 */
    fun dueTone(text: String?): DueTone {
        val days = daysUntil(text) ?: return DueTone.Normal
        return when {
            days < 0 -> DueTone.Overdue
            days <= 7 -> DueTone.Soon
            else -> DueTone.Normal
        }
    }

    enum class DueTone { Normal, Soon, Overdue }
    /** `3 天后到期` / `今天到期` / `已逾期 2 天`。 */
    fun dueLabel(text: String?): String {
        val days = daysUntil(text) ?: return ""
        return when {
            days < 0 -> "已逾期 ${-days} 天"
            days == 0L -> "今天到期"
            else -> "$days 天后到期"
        }
    }

    /**
     * 用 ISBN 拼封面地址。
     *
     * 汇文的图床是固定规则：`bookcover/{前2位}/{次2位}/{13位ISBN}.jpg`，
     * 检索结果里没有封面字段，但每条都带 ISBN，所以不必再为每条结果单独请求封面接口。
     * ISBN 不是 13 位时返回空串，由调用方显示占位底色。
     */
    fun coverUrl(isbn: String?): String {
        val digits = isbn.orEmpty().filter { it.isDigit() }
        if (digits.length != 13) return ""
        return "https://img.hwlsp.cn/bookcover/${digits.take(2)}/${digits.substring(2, 4)}/$digits.jpg"
    }
}
