package com.myhebnu.data.repository

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.myhebnu.data.local.db.dao.GradeDao
import com.myhebnu.data.local.db.entity.GradeEntity
import com.myhebnu.data.local.preferences.UserPreferences
import com.myhebnu.data.remote.EASystemApi
import com.myhebnu.domain.Grade
import com.myhebnu.domain.GradeSubItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GradeRepository @Inject constructor(
    private val api: EASystemApi,
    private val dao: GradeDao,
    private val preferences: UserPreferences
) {
    companion object {
        private const val TAG = "MyHEBNU"
        /** 成绩查询的学期枚举：秋(3) / 春(12)。 */
        private val SEMESTER_TERMS = listOf("3", "12")
    }

    /** All cached grades, newest semester first — the grade screen renders from this. */
    fun observeCachedGrades(): Flow<List<Grade>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    /** True when at least one grade row is cached locally. */
    suspend fun hasCache(): Boolean = dao.count() > 0

    /**
     * Fetch grade list for a given semester.
     * Returns empty list if no grades exist for that semester.
     * Successful non-empty results are written through to Room.
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
            cacheGrades(year, term, grades)
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
     * 首次使用 / 长按强制刷新：拉取近 3 学年 × 秋(3)/春(12) 全部学期。
     * 动态生成年份，避免写死年份跨年后失效。
     */
    suspend fun refreshAllSemesters(): Result<Unit> {
        val thisYear = LocalDate.now().year
        val targets = ((thisYear - 2)..thisYear).flatMap { y ->
            SEMESTER_TERMS.map { term -> y.toString() to term }
        }
        return refreshSemesters(targets)
    }

    /**
     * 短按刷新：只刷新「最近一个有成绩的学期」+ 当前学期。
     *
     * 带上当前学期是为了覆盖"新学期成绩刚公布、缓存里最新学期还停留在上一学期"的情况；
     * 无任何本地缓存时退化为全量刷新。
     */
    suspend fun refreshLatestSemester(): Result<Unit> {
        val latest = latestCachedSemester() ?: return refreshAllSemesters()

        val currentYear = preferences.currentSemesterYear.first()
        val currentTerm = preferences.currentSemesterTerm.first()

        val targets = mutableListOf(latest)
        if (latest.first != currentYear || latest.second != currentTerm) {
            targets.add(currentYear to currentTerm)
        }
        return refreshSemesters(targets)
    }

    /** 本地缓存中最新（学年、学期）——顺序：学年 DESC，学期 3 < 12 < 16。 */
    private suspend fun latestCachedSemester(): Pair<String, String>? {
        val all = dao.getAll()
        if (all.isEmpty()) return null
        val latest = all.maxWithOrNull(
            compareBy<GradeEntity>({ it.semesterYear }, { termRank(it.semesterTerm) })
        ) ?: return null
        return latest.semesterYear to latest.semesterTerm
    }

    /**
     * 逐个学期联网拉取并写穿 Room。
     * 仅当"全部学期都失败"时才向上报错，避免单个学期失败导致整页报错。
     */
    private suspend fun refreshSemesters(targets: List<Pair<String, String>>): Result<Unit> {
        var successCount = 0
        val errors = mutableListOf<Throwable>()
        for ((year, term) in targets) {
            val result = getGrades(year, term)
            if (result.isSuccess) {
                successCount++
            } else {
                result.exceptionOrNull()?.let { errors.add(it) }
            }
        }
        return if (successCount == 0 && errors.isNotEmpty()) {
            Result.failure(errors.first())
        } else {
            Result.success(Unit)
        }
    }

    /**
     * Write-through to Room. 空结果不覆盖本地缓存——偶发的空响应不应抹掉已缓存的成绩。
     */
    private suspend fun cacheGrades(year: String, term: String, grades: List<Grade>) {
        if (grades.isEmpty()) return
        try {
            dao.replaceSemester(year, term, grades.map { it.toEntity() })
        } catch (e: Exception) {
            android.util.Log.w(TAG, "[Grade] Room cache write failed: ${e.message}", e)
        }
    }

    private fun termRank(term: String): Int = when (term) {
        "3" -> 1
        "12" -> 2
        "16" -> 3
        else -> 0
    }

    private fun parseGradeList(items: JsonArray, year: String, term: String): List<Grade> {
        val grades = mutableListOf<Grade>()
        for (i in 0 until items.size()) {
            val item = items[i].asJsonObject
            val semesterName = buildString {
                append(item.get("xnmmc")?.asString ?: "")
                append("-")
                append(item.get("xqmmc")?.asString ?: "")
            }.trimEnd('-')
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
                    semesterName = semesterName.ifBlank { "$year-$term" }
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

    // ── Entity ↔ Domain conversions ──

    private fun Grade.toEntity(): GradeEntity = GradeEntity(
        id = "${semesterYear}-${semesterTerm}-$classId",
        courseName = courseName,
        courseCode = courseCode,
        credit = credit,
        score = score,
        scoreValue = scoreValue,
        classId = classId,
        teachingClassName = teachingClassName,
        department = department,
        semesterYear = semesterYear,
        semesterTerm = semesterTerm,
        semesterName = semesterName,
        lastUpdated = System.currentTimeMillis()
    )

    private fun GradeEntity.toDomain(): Grade = Grade(
        courseName = courseName,
        courseCode = courseCode,
        credit = credit,
        score = score,
        scoreValue = scoreValue,
        classId = classId,
        teachingClassName = teachingClassName,
        department = department,
        semesterYear = semesterYear,
        semesterTerm = semesterTerm,
        semesterName = semesterName
    )
}
