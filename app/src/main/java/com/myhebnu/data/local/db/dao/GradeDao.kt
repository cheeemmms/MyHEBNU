package com.myhebnu.data.local.db.dao

import androidx.room.*
import com.myhebnu.data.local.db.entity.GradeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GradeDao {

    /** All cached grades, newest semester first. Drives the grade screen reactively. */
    @Query("SELECT * FROM grades ORDER BY semesterYear DESC, semesterTerm DESC, courseName ASC")
    fun observeAll(): Flow<List<GradeEntity>>

    @Query("SELECT * FROM grades ORDER BY semesterYear DESC, semesterTerm DESC, courseName ASC")
    suspend fun getAll(): List<GradeEntity>

    @Query("SELECT COUNT(*) FROM grades")
    suspend fun count(): Int

    @Query("SELECT * FROM grades WHERE semesterYear = :year AND semesterTerm = :term ORDER BY courseName ASC")
    suspend fun getBySemester(year: String, term: String): List<GradeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(grades: List<GradeEntity>)

    @Query("DELETE FROM grades WHERE semesterYear = :year AND semesterTerm = :term")
    suspend fun deleteBySemester(year: String, term: String): Int

    /**
     * Atomic transaction: delete-then-insert in one operation.
     * This prevents data loss if delete succeeds but upsert fails.
     */
    @Transaction
    suspend fun replaceSemester(year: String, term: String, grades: List<GradeEntity>) {
        deleteBySemester(year, term)
        upsertAll(grades)
    }
}
