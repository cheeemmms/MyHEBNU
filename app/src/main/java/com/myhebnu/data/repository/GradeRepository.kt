package com.myhebnu.data.repository

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.myhebnu.data.remote.EASystemApi
import com.myhebnu.domain.Grade
import com.myhebnu.domain.GradeSubItem
import java.time.LocalDate
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
            // Step 1: 注册菜单点击
            val menuResult = api.registerMenuClick("N305007")
            if (!menuResult.isSuccessful) {
                return Result.failure(Exception("菜单注册失败: HTTP ${menuResult.code()}"))
            }

            // Step 2: 加载成绩页面（建立浏览器 context — 教务系统门控要求）
            val pageResult = api.loadGradePage()
            if (!pageResult.isSuccessful) {
                return Result.failure(Exception("页面加载失败: HTTP ${pageResult.code()}"))
            }
            val pageBody = pageResult.body()?.string() ?: ""
            if (pageBody.contains("登录") || pageBody.contains("login_slogin")) {
                return Result.failure(Exception("Session 已失效，页面重定向到登录页"))
            }

            // Step 3: 获取成绩数据
            val response = api.getGradeList(year = year, semester = term, showCount = "1000")
            if (!response.isSuccessful) {
                return Result.failure(Exception("HTTP ${response.code()}"))
            }
            val body = response.body() ?: return Result.success(emptyList())
            val items = body.getAsJsonArray("items") ?: JsonArray()
            val grades = parseGradeList(items, year, term)
            Result.success(grades)
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
            // 动态生成近 3 学年 × 秋(3)/春(12)，避免写死年份跨年后失效。
            val thisYear = LocalDate.now().year
            val years = (thisYear - 2)..thisYear
            val terms = listOf("3", "12")
            val allGrades = mutableMapOf<String, List<Grade>>()
            val errors = mutableListOf<Throwable>()

            for (y in years) {
                for (t in terms) {
                    val result = getGrades(y.toString(), t)
                    result.fold(
                        onSuccess = { grades ->
                            if (grades.isNotEmpty()) {
                                // 用接口返回的真实学期名(如 "2025-2026-1")作分组 key
                                val name = grades.first().semesterName.ifBlank { "${y}-${t}" }
                                allGrades[name] = grades
                            }
                        },
                        onFailure = { error -> errors.add(error) }
                    )
                }
            }

            // Only propagate error if ALL calls failed and we have no data
            if (allGrades.isEmpty() && errors.isNotEmpty()) {
                Result.failure(errors.first())
            } else {
                Result.success(allGrades)
            }
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
