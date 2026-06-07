package com.myhebnu.data.repository

import androidx.compose.ui.graphics.toArgb
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.myhebnu.data.local.db.dao.ScheduleDao
import com.myhebnu.data.local.db.entity.CourseEntity
import com.myhebnu.data.local.preferences.UserPreferences
import com.myhebnu.data.remote.EASystemApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleRepository @Inject constructor(
    private val api: EASystemApi,
    private val dao: ScheduleDao,
    private val preferences: UserPreferences
) {
    /**
     * Observe the cached schedule from Room for a given semester.
     * Returns a Flow that emits updates whenever the local cache changes.
     */
    fun observeSchedule(year: String, term: String): Flow<List<CourseEntity>> {
        return dao.getScheduleBySemester(year, term)
    }

    /**
     * Get the schedule for a specific day (for Widget use).
     */
    suspend fun getCoursesByDay(year: String, term: String, day: Int): List<CourseEntity> {
        return dao.getCoursesByDay(year, term, day)
    }

    /**
     * Fetch the latest schedule from the教务系统, parse it,
     * assign colors, and cache to Room.
     *
     * @param year 学年 (e.g. "2025")
     * @param term 学期 (e.g. "12")
     */
    suspend fun refreshSchedule(year: String, term: String): Result<Unit> {
        return try {
            // Step 1: 注册菜单点击
            val menuResult = api.registerMenuClick("N2151")
            if (!menuResult.isSuccessful) {
                return Result.failure(Exception("菜单注册失败: HTTP ${menuResult.code()}"))
            }
            android.util.Log.w("MyHEBNU", "registerMenuClick -> ${menuResult.code()}")

            // Step 2: 加载课表页面（建立浏览器 context）
            val pageResult = api.loadSchedulePage()
            if (!pageResult.isSuccessful) {
                return Result.failure(Exception("页面加载失败: HTTP ${pageResult.code()}"))
            }
            val pageBody = pageResult.body()?.string() ?: ""
            val isLogin = pageBody.contains("登录") || pageBody.contains("login_slogin")
            android.util.Log.w("MyHEBNU", "loadSchedulePage -> ${pageResult.code()}, isLoginPage=$isLogin, size=${pageBody.length}")
            if (isLogin) {
                return Result.failure(Exception("Session 已失效，页面重定向到登录页"))
            }

            // Step 3: 获取课表数据
            val response = api.getSchedule(year = year, semester = term, type = "ck")
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    val raw = body.toString()
                    android.util.Log.w("MyHEBNU", "getSchedule -> ${response.code()}, size=${raw.length}")
                    if (raw.contains("<!doctype") || raw.contains("<html")) {
                        return Result.failure(Exception("服务器返回 HTML 而非 JSON，权限验证失败"))
                    }
                    val courses = parseScheduleResponse(body, year, term)
                    dao.deleteBySemester(year, term)
                    dao.upsertAll(courses)

                    // Extract student name from xsxx and cache for home page greeting
                    val xsxx = body.getAsJsonObject("xsxx")
                    val studentName = xsxx?.get("XM")?.asString
                    if (!studentName.isNullOrBlank()) {
                        preferences.setStudentName(studentName)
                    }

                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Empty response body"))
                }
            } else {
                Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            android.util.Log.e("MyHEBNU", "refreshSchedule 异常: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Parse the教务系统 schedule JSON into [CourseEntity] list.
     *
     * Response structure:
     * {
     *   "kbList": [
     *     {
     *       "kcmc": "中国古代文学史",    // course name
     *       "xm": "李建国",              // teacher
     *       "cdmc": "公共教学楼B座107",   // classroom
     *       "xqj": 1,                     // day of week (1-7)
     *       "jcs": "3-5",                 // period range
     *       "zcd": "1-18周",              // week range text
     *       "kclb": "理论",               // course category
     *       ...
     *     }
     *   ],
     *   "xqjmcMap": { "1":"星期一", ... },
     *   "xsxx": { "XNMC":"2025-2026", ... }
     * }
     */
    private fun parseScheduleResponse(
        body: JsonObject,
        year: String,
        term: String
    ): List<CourseEntity> {
        val kbList: JsonArray = body.getAsJsonArray("kbList") ?: JsonArray()
        val xsxx = body.getAsJsonObject("xsxx")
        val semesterName = buildSemesterName(xsxx)

        // Collect existing course names for color stability
        val courses = mutableListOf<CourseEntity>()

        for (i in 0 until kbList.size()) {
            val item = kbList[i].asJsonObject
            val courseName = item.get("kcmc")?.asString ?: continue
            val teacher = item.get("xm")?.asString
                ?: item.get("jsxm")?.asString ?: ""
            val classroom = item.get("cdmc")?.asString ?: ""
            val dayOfWeek = item.get("xqj")?.asInt ?: continue
            val periodRange = item.get("jcs")?.asString ?: ""
            val weekText = item.get("zcd")?.asString
                ?: item.get("kcxszc")?.asString
                ?: item.get("qsjsz")?.asString ?: ""
            val category = item.get("kclb")?.asString ?: ""

            if (courseName.isBlank() || periodRange.isBlank()) continue

            val (startPeriod, endPeriod) = parsePeriodRange(periodRange)
            val (startWeek, endWeek, oddEven) = parseWeekRange(weekText)

            // Generate a stable ID
            val id = "$year-$term-$courseName-$dayOfWeek-$periodRange-$weekText"

            // Assign a stable hue (0-359) based on course name hash — palette built in ViewModel
            val hue = kotlin.math.abs(courseName.hashCode()) % 360

            courses.add(
                CourseEntity(
                    id = id,
                    courseName = courseName,
                    teacher = teacher,
                    classroom = classroom,
                    dayOfWeek = dayOfWeek,
                    startPeriod = startPeriod,
                    endPeriod = endPeriod,
                    startWeek = startWeek,
                    endWeek = endWeek,
                    oddEven = oddEven,
                    weekText = weekText,
                    category = category,
                    color = hue,
                    semesterYear = year,
                    semesterTerm = term,
                    semesterName = semesterName
                )
            )
        }

        return courses
    }

    /**
     * Parse period range like "3-5" → (3, 5)
     * Also handles "1" → (1, 1)
     */
    private fun parsePeriodRange(text: String): Pair<Int, Int> {
        val parts = text.split("-").mapNotNull { it.trim().toIntOrNull() }
        return when (parts.size) {
            1 -> parts[0] to parts[0]
            2 -> parts[0] to parts[1]
            else -> 1 to 1
        }
    }

    /**
     * Parse week range like "1-18周" → (1, 18, 0)
     * "1-18周(单)" → (1, 18, 1)
     * "1-18周(双)" → (1, 18, 2)
     * Also handles "1-10周,12-18周" → (1, 18, 0) (takes min and max)
     *
     * @return Triple(startWeek, endWeek, oddEven) where oddEven: 0=all, 1=odd, 2=even
     */
    private fun parseWeekRange(text: String): Triple<Int, Int, Int> {
        // Detect odd/even before cleaning
        val oddEven = when {
            text.contains("单") -> 1
            text.contains("双") -> 2
            else -> 0
        }

        val weekNumbers = mutableListOf<Int>()
        // Extract all numbers from the text
        val cleaned = text.replace("周", "").replace("单", "").replace("双", "")
        val segments = cleaned.split(",")
        for (segment in segments) {
            val parts = segment.trim().split("-").mapNotNull { it.trim().toIntOrNull() }
            when (parts.size) {
                1 -> weekNumbers.add(parts[0])
                2 -> {
                    for (w in parts[0]..parts[1]) {
                        weekNumbers.add(w)
                    }
                }
            }
        }
        val (minWeek, maxWeek) = if (weekNumbers.isEmpty()) {
            1 to 18
        } else {
            weekNumbers.min() to weekNumbers.max()
        }
        return Triple(minWeek, maxWeek, oddEven)
    }

    /**
     * Fetch week number → date range mapping from the N2154 API.
     *
     * Uses the three-step request sequence (menu click → page load → data) to
     * satisfy the教务系统's access-control requirements.
     *
     * @param year 学年 (e.g. "2025")
     * @param term 学期 (e.g. "12")
     * @return Result with Map<weekNumber, dateRange>, e.g. {1: "2025-09-08/2025-09-14"}
     */
    suspend fun fetchWeekDateMapping(year: String, term: String): Result<Map<Int, String>> {
        return try {
            // Step 1: 注册菜单点击
            val menuResult = api.registerMenuClick("N2154")
            android.util.Log.w("MyHEBNU", "N2154 registerMenuClick -> ${menuResult.code()}")

            // Step 2: 加载周次课表页面
            val pageResult = api.loadWeekSchedulePage()
            android.util.Log.w("MyHEBNU", "N2154 loadWeekSchedulePage -> ${pageResult.code()}")

            // Step 3: 获取周次日期映射
            val response = api.getWeeksBySemester(year = year, semester = term)
            if (!response.isSuccessful) {
                return Result.failure(Exception("getWeeksBySemester HTTP ${response.code()}"))
            }

            val rawJson = response.body()?.string() ?: ""
            if (rawJson.isBlank()) {
                return Result.failure(Exception("getWeeksBySemester empty body"))
            }
            if (rawJson.contains("<!doctype") || rawJson.contains("<html")) {
                return Result.failure(Exception("getWeeksBySemester returned HTML, permission denied"))
            }

            val mapping = parseWeekListFromRaw(rawJson)
            android.util.Log.w("MyHEBNU", "N2154 week mapping: $mapping")
            Result.success(mapping)
        } catch (e: Exception) {
            android.util.Log.e("MyHEBNU", "fetchWeekDateMapping error", e)
            Result.failure(e)
        }
    }

    /**
     * Parse the N2154 week list JSON into a mapping of week number → date range.
     *
     * Response is a bare JSON array:
     * [{"zs": 1, "rq": "2025-09-08/2025-09-14", "zsmc": "1", ...}, ...]
     */
    private fun parseWeekListFromRaw(rawJson: String): Map<Int, String> {
        val result = mutableMapOf<Int, String>()
        try {
            val array = com.google.gson.JsonParser.parseString(rawJson).asJsonArray
            for (item in array) {
                val obj = item.asJsonObject
                val zs = obj.get("zs")?.asInt ?: continue
                val rq = obj.get("rq")?.asString ?: continue
                result[zs] = rq
            }
        } catch (e: Exception) {
            android.util.Log.e("MyHEBNU", "parseWeekListFromRaw error", e)
        }
        return result
    }

    /**
     * Build human-readable semester name from xsxx data.
     */
    private fun buildSemesterName(xsxx: JsonObject?): String {
        if (xsxx == null) return ""
        val yearName = xsxx.get("XNMC")?.asString ?: ""
        val termName = xsxx.get("XQMMC")?.asString ?: ""
        return if (yearName.isNotBlank() && termName.isNotBlank()) {
            "$yearName-$termName"
        } else {
            ""
        }
    }

    /**
     * Get the current semester info from preferences.
     */
    suspend fun getCurrentSemester(): Pair<String, String> {
        val year = preferences.currentSemesterYear.first()
        val term = preferences.currentSemesterTerm.first()
        return year to term
    }

    /**
     * Check if there's cached data for a semester (for offline support).
     */
    suspend fun hasCachedData(year: String, term: String): Boolean {
        return dao.getCourseListBySemester(year, term).isNotEmpty()
    }
}
