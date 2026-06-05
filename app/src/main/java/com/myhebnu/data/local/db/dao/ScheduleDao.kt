package com.myhebnu.data.local.db.dao

import androidx.room.*
import com.myhebnu.data.local.db.entity.CourseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {

    @Query("SELECT * FROM courses WHERE semesterYear = :year AND semesterTerm = :term ORDER BY dayOfWeek ASC, startPeriod ASC")
    fun getScheduleBySemester(year: String, term: String): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE semesterYear = :year AND semesterTerm = :term AND dayOfWeek = :day ORDER BY startPeriod ASC")
    suspend fun getCoursesByDay(year: String, term: String, day: Int): List<CourseEntity>

    @Query("SELECT * FROM courses WHERE semesterYear = :year AND semesterTerm = :term")
    suspend fun getCourseListBySemester(year: String, term: String): List<CourseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(courses: List<CourseEntity>): List<Long>

    @Query("DELETE FROM courses WHERE semesterYear = :year AND semesterTerm = :term")
    suspend fun deleteBySemester(year: String, term: String): Int

    @Query("DELETE FROM courses")
    suspend fun deleteAll(): Int

    @Query("SELECT DISTINCT semesterYear, semesterTerm, semesterName FROM courses ORDER BY semesterYear DESC, semesterTerm DESC")
    suspend fun getAvailableSemesters(): List<SemesterTuple>
}

data class SemesterTuple(
    val semesterYear: String,
    val semesterTerm: String,
    val semesterName: String
)
