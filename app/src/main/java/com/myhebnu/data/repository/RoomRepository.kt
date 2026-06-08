package com.myhebnu.data.repository

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.myhebnu.data.remote.EASystemApi
import com.myhebnu.domain.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomRepository @Inject constructor(
    private val api: EASystemApi
) {
    companion object {
        /** Quick check: response body is not raw HTML or error text */
        fun isHtmlResponse(text: String): Boolean {
            return text.contains("<!doctype") || text.contains("<html") ||
                text.contains("无功能权限") || text.contains("登录") ||
                text.contains("必选字段")
        }
    }

    /**
     * Read raw response body as string, validate it's not HTML/error, then parse as JSON.
     * Throws on failure — let the caller's try/catch handle it naturally.
     */
    private fun readJsonBody(rawBody: okhttp3.ResponseBody?, tag: String): JsonObject {
        val text = rawBody?.string()
            ?: throw Exception("$tag: response body is null")
        android.util.Log.w("MyHEBNU", "$tag raw=${text.take(200)}")
        if (isHtmlResponse(text)) {
            android.util.Log.e("MyHEBNU", "$tag: HTML/error response detected")
            throw Exception("教务系统拒绝请求（返回HTML或错误页）")
        }
        return try {
            JsonParser.parseString(text).asJsonObject
        } catch (e: Exception) {
            android.util.Log.e("MyHEBNU", "$tag: JSON parse failed: ${e.message}")
            throw Exception("JSON解析失败: ${e.message}")
        }
    }

    /**
     * Fetch campus buildings and period information.
     * Includes the 3-step 门控 sequence (menu click → page load → data fetch).
     */
    suspend fun getCampusInfo(
        campusId: String,
        year: String,
        term: String
    ): Result<CampusInfo> {
        return try {
            android.util.Log.w("MyHEBNU", "getCampusInfo: campus=$campusId year=$year term=$term")

            // Step 1: 注册菜单点击
            val menuResult = api.registerMenuClick("N2155")
            android.util.Log.w("MyHEBNU", "getCampusInfo menu: HTTP ${menuResult.code()}")
            if (!menuResult.isSuccessful) {
                return Result.failure(Exception("菜单注册失败: HTTP ${menuResult.code()}"))
            }

            // Step 2: 加载空教室页面（建立浏览器 context）
            val pageResult = api.loadRoomPage()
            android.util.Log.w("MyHEBNU", "getCampusInfo page: HTTP ${pageResult.code()}")
            if (!pageResult.isSuccessful) {
                return Result.failure(Exception("页面加载失败: HTTP ${pageResult.code()}"))
            }

            // Step 3: 获取校区楼栋数据
            val response = api.getCampusBuildingInfo(campusId, year, term)
            android.util.Log.w("MyHEBNU", "getCampusInfo HTTP ${response.code()}")

            if (!response.isSuccessful) {
                return Result.failure(Exception("HTTP ${response.code()}"))
            }

            val body = readJsonBody(response.body(), "getCampusInfo")
            val buildings = mutableListOf<Building>()
            val lhList = body.getAsJsonArray("lhList")
            if (lhList != null) {
                for (i in 0 until lhList.size()) {
                    val item = lhList[i].asJsonObject
                    buildings.add(
                        Building(
                            code = item.get("JXLDM")?.asString ?: "",
                            name = item.get("JXLMC")?.asString ?: ""
                        )
                    )
                }
            }

            val periods = mutableListOf<PeriodSlot>()
            val jcList = body.getAsJsonArray("jcList")
            if (jcList != null) {
                for (i in 0 until jcList.size()) {
                    val item = jcList[i].asJsonObject
                    periods.add(
                        PeriodSlot(
                            name = item.get("jcmc")?.asString ?: "",
                            startTime = item.get("qssj")?.asString ?: "",
                            endTime = item.get("jssj")?.asString ?: ""
                        )
                    )
                }
            }

            Result.success(
                CampusInfo(
                    campusId = campusId,
                    buildings = buildings,
                    periods = periods
                )
            )
        } catch (e: Exception) {
            android.util.Log.e("MyHEBNU", "getCampusInfo exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Query empty classrooms with the given filter.
     */
    suspend fun queryEmptyRooms(filter: RoomFilter, page: Int = 1): Result<RoomQueryResult> {
        return try {
            // Step 1: 注册菜单点击
            android.util.Log.w("MyHEBNU", "queryEmptyRooms step1: registerMenuClick")
            val menuResult = api.registerMenuClick("N2155")
            android.util.Log.w("MyHEBNU", "queryEmptyRooms step1: HTTP ${menuResult.code()}")
            if (!menuResult.isSuccessful) {
                return Result.failure(Exception("菜单注册失败: HTTP ${menuResult.code()}"))
            }

            // Step 2: 加载空教室页面（建立浏览器 context——教务系统门控要求）
            android.util.Log.w("MyHEBNU", "queryEmptyRooms step2: loadRoomPage")
            val pageResult = api.loadRoomPage()
            android.util.Log.w("MyHEBNU", "queryEmptyRooms step2: HTTP ${pageResult.code()}")
            if (!pageResult.isSuccessful) {
                return Result.failure(Exception("页面加载失败: HTTP ${pageResult.code()}"))
            }
            val pageBody = pageResult.body()?.string() ?: ""
            val isLogin = pageBody.contains("登录") || pageBody.contains("login_slogin")
            android.util.Log.w("MyHEBNU", "queryEmptyRooms step2: body size=${pageBody.length}, isLogin=$isLogin")
            if (isLogin) {
                return Result.failure(Exception("Session 已失效，页面重定向到登录页"))
            }

            // Step 3: 查询空教室数据
            android.util.Log.w("MyHEBNU", "queryEmptyRooms step3: executing...")
            // Determine building param
            val lh = filter.building ?: ""

            // Determine week mask
            val zcd = filter.weekMask
                ?: filter.weekNumber?.let { BitmaskUtil.weekToMask(it) }
                ?: ""

            // Determine day of week
            val xqj = filter.dayOfWeek ?: ""

            // Determine period mask
            val jcd = filter.periodMask ?: ""

            val response = api.getEmptyRooms(
                campusId = filter.campusId,
                year = filter.year,
                semester = filter.term,
                venueType = filter.venueTypeId,
                building = lh,
                weekBitmask = zcd,
                dayOfWeek = xqj,
                periodBitmask = jcd,
                minSeats = filter.minSeats ?: "",
                maxSeats = filter.maxSeats ?: "",
                roomName = filter.roomName ?: "",
                currentPage = page.toString(),
                pageSize = "20"
            )

            android.util.Log.w("MyHEBNU", "queryEmptyRooms HTTP ${response.code()}")

            if (!response.isSuccessful) {
                return Result.failure(Exception("HTTP ${response.code()}"))
            }

            val body = readJsonBody(response.body(), "queryEmptyRooms")

            val items = body.getAsJsonArray("items")
            val rooms = parseEmptyRooms(items ?: JsonArray())
            val totalCount = body.get("totalCount")?.asInt ?: rooms.size
            val totalPage = body.get("totalPage")?.asInt
                ?: ((totalCount + 19) / 20)  // fallback: calculate from totalCount at 20/page
            android.util.Log.w("MyHEBNU", "queryEmptyRooms page=$page: ${rooms.size} rooms, totalCount=$totalCount, totalPage=$totalPage")
            Result.success(RoomQueryResult(rooms = rooms, totalCount = totalCount, totalPage = totalPage))
        } catch (e: Exception) {
            android.util.Log.e("MyHEBNU", "queryEmptyRooms exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get the week description for displaying the selected week.
     */
    suspend fun getWeekDescription(
        year: String,
        term: String,
        weekMask: String
    ): Result<String> {
        return try {
            val response = api.getWeekDescription(year, term, weekMask)
            if (response.isSuccessful) {
                Result.success(response.body() ?: "")
            } else {
                Result.success("") // Non-critical
            }
        } catch (e: Exception) {
            Result.success("")
        }
    }

    /**
     * Get the period description for displaying the selected period.
     */
    suspend fun getPeriodDescription(
        periodMask: String
    ): Result<String> {
        return try {
            val response = api.getPeriodDescription(periodMask)
            if (response.isSuccessful) {
                Result.success(response.body() ?: "")
            } else {
                Result.success("")
            }
        } catch (e: Exception) {
            Result.success("")
        }
    }

    private fun parseEmptyRooms(items: JsonArray): List<EmptyRoom> {
        val rooms = mutableListOf<EmptyRoom>()
        for (i in 0 until items.size()) {
            val item = items[i].asJsonObject
            rooms.add(
                EmptyRoom(
                    roomName = item.get("cdmc")?.asString
                        ?: item.get("cdbh")?.asString ?: "",
                    roomCode = item.get("cdbh")?.asString ?: "",
                    building = item.get("lh")?.asString ?: "",
                    buildingName = item.get("jxlmc")?.asString
                        ?: item.get("jgmc")?.asString ?: "",
                    floor = item.get("lch")?.asString,
                    seats = item.get("zws")?.asString
                        ?: item.get("kszws1")?.asString,
                    venueType = item.get("cdlbmc")?.asString,
                    campusName = item.get("xqmc")?.asString,
                    usageType = item.get("cdjylx")?.asString,
                    area = item.get("jzmj")?.asString
                )
            )
        }
        return rooms
    }
}
