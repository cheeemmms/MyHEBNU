package com.myhebnu.data.repository

import android.util.Log
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.myhebnu.data.remote.EASystemApi
import com.myhebnu.domain.Exam
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExamRepository @Inject constructor(
    private val api: EASystemApi
) {
    companion object {
        private const val TAG = "MyHEBNU"
        private const val MODULE_CODE = "N358105"
    }

    /**
     * Fetch exam schedule for the given academic year and semester.
     * Returns exams sorted by date (nearest first).
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

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    // Guard against HTML responses (server rejects the request)
                    val bodyStr = body.toString()
                    if (bodyStr.contains("<!doctype", ignoreCase = true) ||
                        bodyStr.contains("<html", ignoreCase = true) ||
                        bodyStr.contains("无功能权限")
                    ) {
                        Log.e(TAG, "[Exam] Server returned HTML instead of JSON")
                        return Result.failure(Exception("教务系统拒绝请求，请重新登录后重试"))
                    }

                    val items = body.getAsJsonArray("items")
                    val exams = parseExamList(items ?: JsonArray())
                    Log.d(TAG, "[Exam] Parsed ${exams.size} exams")
                    Result.success(exams.sortedBy { it.examDate })
                } else {
                    Log.d(TAG, "[Exam] Empty response body")
                    Result.success(emptyList())
                }
            } else {
                Log.w(TAG, "[Exam] HTTP ${response.code()}")
                Result.failure(Exception("HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "[Exam] Exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun parseExamList(items: JsonArray): List<Exam> {
        val exams = mutableListOf<Exam>()
        for (i in 0 until items.size()) {
            val item = items[i].asJsonObject
            val kssj = item.get("kssj")?.asString ?: ""
            val (date, start, end) = Exam.parseExamDateTime(kssj)

            exams.add(
                Exam(
                    courseName = item.get("kcmc")?.asString ?: "",
                    examDate = date ?: java.time.LocalDate.now(),
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
}
