package com.myhebnu.data.repository

import androidx.compose.ui.graphics.toArgb
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.myhebnu.data.local.db.dao.ScheduleDao
import com.myhebnu.data.local.db.entity.CourseEntity
import com.myhebnu.data.local.preferences.UserPreferences
import com.myhebnu.data.remote.EASystemApi
import com.myhebnu.ui.theme.CourseColors
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
            val response = api.getSchedule(
                year = year,
                semester = term,
                type = "ck"
            )
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    val courses = parseScheduleResponse(body, year, term)
                    // Replace cached courses for this semester atomically
                    dao.deleteBySemester(year, term)
                    dao.upsertAll(courses)
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Empty response body"))
                }
            } else {
                Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
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
            val (startWeek, endWeek) = parseWeekRange(weekText)

            // Generate a stable ID
            val id = "$year-$term-$courseName-$dayOfWeek-$periodRange-$weekText"

            // Assign a stable color based on course name hash
            val colorIndex = kotlin.math.abs(courseName.hashCode()) % CourseColors.size
            val color = CourseColors[colorIndex].toArgb()

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
                    weekText = weekText,
                    category = category,
                    color = color,
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
     * Parse week range like "1-18周" → (1, 18)
     * Also handles "1-10周,12-18周" → (1, 18) (takes min and max)
     */
    private fun parseWeekRange(text: String): Pair<Int, Int> {
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
        return if (weekNumbers.isEmpty()) {
            1 to 18
        } else {
            weekNumbers.min() to weekNumbers.max()
        }
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
