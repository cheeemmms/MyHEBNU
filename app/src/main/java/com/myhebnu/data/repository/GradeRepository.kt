package com.myhebnu.data.repository

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.myhebnu.data.remote.EASystemApi
import com.myhebnu.domain.Grade
import com.myhebnu.domain.GradeSubItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GradeRepository @Inject constructor(
    private val api: EASystemApi
) {
    /**
     * Fetch grade list for a given semester.
     * Returns empty list if no grades exist for that semester.
     */
    suspend fun getGrades(year: String, term: String): Result<List<Grade>> {
        return try {
            api.registerMenuClick("N305007")
            val response = api.getGradeList(year = year, semester = term)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    val items = body.getAsJsonArray("items")
                    val grades = parseGradeList(items ?: JsonArray(), year, term)
                    Result.success(grades)
                } else {
                    Result.success(emptyList())
                }
            } else {
                Result.failure(Exception("HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch detailed grade breakdown for a specific course.
     * Returns sub-items like "课堂表现(20%)=100", "期末考试(50%)=86".
     */
    suspend fun getGradeDetail(
        year: String,
        term: String,
        classId: String
    ): Result<List<GradeSubItem>> {
        return try {
            val response = api.getGradeDetail(year = year, semester = term, classId = classId)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    val items = body.getAsJsonArray("items")
                    val details = parseGradeDetails(items ?: JsonArray())
                    Result.success(details)
                } else {
                    Result.success(emptyList())
                }
            } else {
                Result.failure(Exception("HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch grades for all available semesters by querying known semesters.
     */
    suspend fun getAllGrades(): Result<Map<String, List<Grade>>> {
        return try {
            // Query the common semesters (2024-1, 2024-2, 2025-1, 2025-2)
            val semesterParams = listOf(
                Triple("2024", "3", "2024-2025-1"),
                Triple("2024", "12", "2024-2025-2"),
                Triple("2025", "3", "2025-2026-1"),
                Triple("2025", "12", "2025-2026-2")
            )
            val allGrades = mutableMapOf<String, List<Grade>>()

            for ((year, term, name) in semesterParams) {
                val result = getGrades(year, term)
                result.onSuccess { grades ->
                    if (grades.isNotEmpty()) {
                        allGrades[name] = grades
                    }
                }
            }
            Result.success(allGrades)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseGradeList(items: JsonArray, year: String, term: String): List<Grade> {
        val grades = mutableListOf<Grade>()
        for (i in 0 until items.size()) {
            val item = items[i].asJsonObject
            grades.add(
                Grade(
                    courseName = item.get("kcmc")?.asString ?: "",
                    courseCode = item.get("kch")?.asString ?: "",
                    credit = item.get("xf")?.asFloat ?: 0f,
                    score = item.get("zpcj")?.asString
                        ?: item.get("zpcj")?.asFloat?.toString() ?: "",
                    scoreValue = parseScore(item),
                    classId = item.get("jxb_id")?.asString ?: "",
                    teachingClassName = item.get("jxbmc")?.asString ?: "",
                    department = item.get("kkbmmc")?.asString ?: "",
                    semesterYear = year,
                    semesterTerm = term,
                    semesterName = buildString {
                        append(item.get("xnmmc")?.asString ?: "")
                        append("-")
                        append(item.get("xqmmc")?.asString ?: "")
                    }
                )
            )
        }
        return grades
    }

    private fun parseScore(item: JsonObject): Float? {
        val scoreStr = item.get("zpcj")?.asString
        if (scoreStr != null) {
            return scoreStr.toFloatOrNull()
        }
        val scoreNum = item.get("zpcj")?.asFloat
        return scoreNum
    }

    private fun parseGradeDetails(items: JsonArray): List<GradeSubItem> {
        val details = mutableListOf<GradeSubItem>()
        for (i in 0 until items.size()) {
            val item = items[i].asJsonObject
            val name = item.get("xmblmc")?.asString ?: continue
            val score = item.get("xmcj")?.asString
                ?: item.get("xmcj")?.asFloat?.toString() ?: ""
            details.add(GradeSubItem(name = name, score = score))
        }
        return details
    }
}
