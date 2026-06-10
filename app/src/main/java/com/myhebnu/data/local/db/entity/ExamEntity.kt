package com.myhebnu.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for cached exam data.
 *
 * Primary key includes semester year and term to guarantee uniqueness across semesters.
 * The index on (semesterYear, semesterTerm) supports the common query pattern.
 */
@Entity(
    tableName = "exams",
    indices = [Index(value = ["semesterYear", "semesterTerm"])]
)
data class ExamEntity(
    @PrimaryKey
    val id: String,                    // "$year-$term-$sjbh" or fallback
    val courseName: String,
    val examDate: String,              // "yyyy-MM-dd"
    val startTime: String?,            // "HH:mm", nullable
    val endTime: String?,              // "HH:mm", nullable
    val location: String,
    val campus: String,
    val seatNumber: String,
    val examType: String,
    val department: String,
    val className: String,
    val teacherInfo: String,
    val courseSchedule: String,
    val examMethod: String,
    val semesterYear: String,
    val semesterTerm: String,
    val lastUpdated: Long = 0L
)
