package com.myhebnu.data.repository

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.myhebnu.data.remote.EASystemApi
import com.myhebnu.domain.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomRepository @Inject constructor(
    private val api: EASystemApi
) {
    /**
     * Fetch campus buildings and period information.
     */
    suspend fun getCampusInfo(
        campusId: String,
        year: String,
        term: String
    ): Result<CampusInfo> {
        return try {
            val response = api.getCampusBuildingInfo(campusId, year, term)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
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
                } else {
                    Result.failure(Exception("Empty response"))
                }
            } else {
                Result.failure(Exception("HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Query empty classrooms with the given filter.
     */
    suspend fun queryEmptyRooms(filter: RoomFilter): Result<List<EmptyRoom>> {
        return try {
            api.registerMenuClick("N2155")
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
                roomName = filter.roomName ?: ""
            )

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    val items = body.getAsJsonArray("items")
                    val rooms = parseEmptyRooms(items ?: JsonArray())
                    Result.success(rooms)
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
