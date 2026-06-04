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
    val weekText: String,                // 周次描述文本 (如"1-18周")
    val category: String,                // 课程类别 (kclb: 理论/实践/实验)
    val color: Int,                      // 课程颜色 (ARGB, 由APP分配)
    val semesterYear: String,            // 学年 (xnm: "2025")
    val semesterTerm: String,            // 学期 (xqm: "12")
    val semesterName: String,            // 学期名称 (如"2025-2026-2")
    val lastUpdated: Long = 0L
)
