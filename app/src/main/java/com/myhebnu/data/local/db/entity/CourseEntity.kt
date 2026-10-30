package com.myhebnu.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey
    val id: String,                      // 教务系统课程唯一标识(拼接)
    val courseName: String,              // 课程名称 (kcmc)
    val teacher: String,                 // 教师姓名 (xm/jsxm)
    val classroom: String,               // 教室名称 (cdmc)
    val dayOfWeek: Int,                  // 星期几 (xqj: 1-7)
    val startPeriod: Int,                // 开始节次
    val endPeriod: Int,                  // 结束节次
    val startWeek: Int,                  // 起始教学周
    val endWeek: Int,                    // 结束教学周
    val oddEven: Int = 0,               // 单双周: 0=全周, 1=仅单周, 2=仅双周
    /** 周次位掩码（教务 oldzc，bit0=第1周）。非 0 时为权威判定依据；0 表示老数据，回落 startWeek..endWeek。 */
    val weekBits: Int = 0,
    /** 节次位掩码（教务 oldjc，bit0=第1节）。0 表示老数据，回落 startPeriod..endPeriod。 */
    val periodBits: Int = 0,
    val weekText: String,                // 周次描述文本 (如"1-18周")
    val category: String,                // 课程类别 (kclb: 理论/实践/实验)
    val color: Int,                      // 课程颜色 (ARGB, 由APP分配)
    val semesterYear: String,            // 学年 (xnm: "2025")
    val semesterTerm: String,            // 学期 (xqm: "12")
    val semesterName: String,            // 学期名称 (如"2025-2026-2")
    val lastUpdated: Long = 0L
)

/**
 * 该课程在第 [week] 教学周是否上课——全项目唯一的周次判定入口。
 *
 * 有周次位掩码时以掩码为准：教务的 `oldzc` 已把「多段」「段级单双」「第 N 周」
 * 等所有写法编码成精确的周集合，比解析 `zcd` 文本可靠。
 * 掩码为 0（引入掩码前写入的老数据）才回落到 `startWeek..endWeek` + `oddEven`。
 */
fun CourseEntity.isActiveInWeek(week: Int): Boolean {
    if (weekBits != 0) {
        if (week !in 1..31) return false
        return ((weekBits shr (week - 1)) and 1) == 1
    }
    if (week !in startWeek..endWeek) return false
    return when (oddEven) {
        1 -> week % 2 == 1
        2 -> week % 2 == 0
        else -> true
    }
}
