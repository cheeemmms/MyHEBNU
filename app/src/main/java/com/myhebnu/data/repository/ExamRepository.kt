package com.myhebnu.data.repository

import android.util.Log
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.myhebnu.data.local.db.dao.ExamDao
import com.myhebnu.data.local.db.entity.ExamEntity
import com.myhebnu.data.remote.EASystemApi
import com.myhebnu.domain.Exam
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExamRepository @Inject constructor(
    private val api: EASystemApi,
    private val dao: ExamDao
) {
    companion object {
        private const val TAG = "MyHEBNU"
        private const val MODULE_CODE = "N358105"
    }

    /**
     * Fetch exam schedule for the given academic year and semester.
     * Returns exams sorted by date (nearest first).
     */
    /**
     * Fetch exam schedule from the EA system and write-through to Room.
     * On success, the result is cached atomically so the Reminder Worker
     * can read it without any API calls.
     */
    suspend fun getExams(year: String, term: String): Result<List<Exam>> {
        return try {
            // Step 1: Register menu click for exam module
            Log.d(TAG, "[Exam] Step 1: registerMenuClick($MODULE_CODE)")
            api.registerMenuClick(MODULE_CODE)

            // Step 2: Fetch exam data
            Log.d(TAG, "[Exam] Step 2: getExams(year=$year, term=$term)")
            val response = api.getExams(
                year = year,
                semester = term
            )

            val result: Result<List<Exam>> = if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    // Guard against HTML responses (server rejects the request)
                    val bodyStr = body.toString()
                    if (bodyStr.contains("<!doctype", ignoreCase = true) ||
                        bodyStr.contains("<html", ignoreCase = true) ||
                        bodyStr.contains("无功能权限")
                    ) {
                        Log.e(TAG, "[Exam] Server returned HTML instead of JSON")
                        Result.failure(Exception("教务系统拒绝请求，请重新登录后重试"))
                    } else {
                        val items = body.getAsJsonArray("items")
                        val exams = parseExamList(year, term, items ?: JsonArray())
                        Log.d(TAG, "[Exam] Parsed ${exams.size} exams")
                        Result.success(exams.sortedBy { it.examDate })
                    }
                } else {
                    Log.d(TAG, "[Exam] Empty response body")
                    Result.success(emptyList())
                }
            } else {
                Log.w(TAG, "[Exam] HTTP ${response.code()}")
                Result.failure(Exception("HTTP ${response.code()}"))
            }

            // Step 3: Write-through to Room on success
            result.onSuccess { exams ->
                try {
                    dao.replaceSemester(year, term, exams.map { it.toEntity(year, term) })
                    Log.d(TAG, "[Exam] Cached ${exams.size} exams to Room")
                } catch (e: Exception) {
                    Log.w(TAG, "[Exam] Room cache write failed: ${e.message}", e)
                }
            }

            result
        } catch (e: Exception) {
            Log.e(TAG, "[Exam] Exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    /** Read cached exams from Room (for Worker — zero API calls). */
    suspend fun getCachedExams(year: String, term: String): List<Exam> {
        return dao.getExamsBySemester(year, term).map { it.toDomain() }
    }

    /** Observe cached exams from Room (for ViewModel — reactive). */
    fun observeExams(year: String, term: String): Flow<List<Exam>> {
        return dao.observeExams(year, term).map { list -> list.map { it.toDomain() } }
    }

    private fun parseExamList(year: String, term: String, items: JsonArray): List<Exam> {
        val exams = mutableListOf<Exam>()
        for (i in 0 until items.size()) {
            val item = items[i].asJsonObject
            val kssj = item.get("kssj")?.asString ?: ""
            val (date, start, end) = Exam.parseExamDateTime(kssj)

            val courseName = item.get("kcmc")?.asString ?: ""
            val examDate = date ?: java.time.LocalDate.now()

            // Construct stable ID: prefer native sjbh with semester prefix for cross-semester uniqueness
            val sjbh = item.get("sjbh")?.asString ?: ""
            val id = if (sjbh.isNotBlank()) "${year}-${term}-${sjbh}"
                     else "${year}-${term}-${courseName}-${examDate}"

            exams.add(
                Exam(
                    id = id,
                    courseName = courseName,
                    examDate = examDate,
                    startTime = start,
                    endTime = end,
                    location = item.get("cdmc")?.asString ?: "",
                    campus = item.get("cdxqmc")?.asString ?: "",
                    seatNumber = item.get("zwh")?.asString ?: "",
                    examType = item.get("ksmc")?.asString ?: "",
                    department = item.get("kkxy")?.asString ?: "",
                    className = item.get("bj")?.asString ?: "",
                    teacherInfo = item.get("jsxx")?.asString ?: "",
                    courseSchedule = item.get("sksj")?.asString ?: "",
                    examMethod = item.get("ksfs")?.asString ?: ""
                )
            )
        }
        return exams
    }

    // ── Entity ↔ Domain conversions ──

    private fun ExamEntity.toDomain(): Exam = Exam(
        id = this.id,
        courseName = this.courseName,
        examDate = LocalDate.parse(this.examDate),
        startTime = this.startTime?.let { LocalTime.parse(it) },
        endTime = this.endTime?.let { LocalTime.parse(it) },
        location = this.location,
        campus = this.campus,
        seatNumber = this.seatNumber,
        examType = this.examType,
        department = this.department,
        className = this.className,
        teacherInfo = this.teacherInfo,
        courseSchedule = this.courseSchedule,
        examMethod = this.examMethod
    )

    private fun Exam.toEntity(year: String, term: String): ExamEntity = ExamEntity(
        id = this.id,
        courseName = this.courseName,
        examDate = this.examDate.toString(),
        startTime = this.startTime?.toString(),
        endTime = this.endTime?.toString(),
        location = this.location,
        campus = this.campus,
        seatNumber = this.seatNumber,
        examType = this.examType,
        department = this.department,
        className = this.className,
        teacherInfo = this.teacherInfo,
        courseSchedule = this.courseSchedule,
        examMethod = this.examMethod,
        semesterYear = year,
        semesterTerm = term,
        lastUpdated = System.currentTimeMillis()
    )
}
