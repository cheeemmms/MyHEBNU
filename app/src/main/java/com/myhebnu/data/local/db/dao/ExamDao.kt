package com.myhebnu.data.local.db.dao

import androidx.room.*
import com.myhebnu.data.local.db.entity.ExamEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamDao {

    @Query("SELECT * FROM exams WHERE semesterYear = :year AND semesterTerm = :term ORDER BY examDate ASC")
    fun observeExams(year: String, term: String): Flow<List<ExamEntity>>

    @Query("SELECT * FROM exams WHERE semesterYear = :year AND semesterTerm = :term ORDER BY examDate ASC")
    suspend fun getExamsBySemester(year: String, term: String): List<ExamEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(exams: List<ExamEntity>)

    @Query("DELETE FROM exams WHERE semesterYear = :year AND semesterTerm = :term")
    suspend fun deleteBySemester(year: String, term: String): Int

    /**
     * Atomic transaction: delete-then-insert in one operation.
     * This prevents data loss if delete succeeds but upsert fails.
     */
    @Transaction
    suspend fun replaceSemester(year: String, term: String, exams: List<ExamEntity>) {
        deleteBySemester(year, term)
        upsertAll(exams)
    }
}
