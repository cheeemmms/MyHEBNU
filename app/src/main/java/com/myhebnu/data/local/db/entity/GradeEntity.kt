package com.myhebnu.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for cached grade data.
 *
 * Primary key is "$semesterYear-$semesterTerm-$classId" so the same teaching class
 * stays unique even if it is retaken in a later semester. The index on
 * (semesterYear, semesterTerm) supports the common per-semester queries.
 */
@Entity(
    tableName = "grades",
    indices = [Index(value = ["semesterYear", "semesterTerm"])]
)
data class GradeEntity(
    @PrimaryKey
    val id: String,                    // "$semesterYear-$semesterTerm-$classId"
    val courseName: String,            // kcmc
    val courseCode: String,            // kch
    val credit: Float,                 // xf
    val score: String,                 // zpcj
    val scoreValue: Float?,            // parsed numeric score for GPA calc, null when ungraded
    val classId: String,               // jxb_id
    val teachingClassName: String,     // jxbmc
    val department: String,            // kkbmmc
    val semesterYear: String,          // xnm
    val semesterTerm: String,          // xqm
    val semesterName: String,          // "2025-2026-2"
    val lastUpdated: Long = 0L
)
