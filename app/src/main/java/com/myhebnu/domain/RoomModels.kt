package com.myhebnu.domain

/**
 * Building information for a campus.
 */
data class Building(
    val code: String,       // JXLDM — building code (e.g. "01")
    val name: String        // JXLMC — building name (e.g. "公共教学楼A座")
)

/**
 * Period (class session) time slot.
 */
data class PeriodSlot(
    val name: String,       // jcmc — period number (e.g. "1", "2")
    val startTime: String,  // qssj — start time (e.g. "08:00")
    val endTime: String     // jssj — end time (e.g. "08:40")
)

/**
 * Campus information from /cdjy/cdjy_cxXqjc.html
 */
data class CampusInfo(
    val campusId: String,
    val buildings: List<Building>,
    val periods: List<PeriodSlot>
)

/**
 * An available classroom.
 */
data class EmptyRoom(
    val roomName: String,       // cdmc — full name
    val roomCode: String,       // cdbh — short code
    val building: String,       // lh — building number
    val buildingName: String,   // jxlmc — building display name
    val floor: String?,         // lch — floor
    val seats: String?,         // zws — seat count
    val venueType: String?,     // cdlbmc — venue type
    val campusName: String?,    // xqmc — campus name
    val usageType: String?,     // cdjylx — usage categories
    val area: String?           // jzmj — building area
)

/**
 * Empty classroom query filter parameters.
 */
data class RoomFilter(
    val campusId: String = "4",
    val year: String = "2025",
    val term: String = "12",
    val venueTypeId: String = "02",
    val building: String? = null,       // lh — null = all buildings
    val weekMask: String? = null,        // zcd — bitmask
    val weekNumber: Int? = null,         // 1-20, converted to zcd bitmask
    val dayOfWeek: String? = null,       // xqj — e.g. "1" or "1,2,3"
    val periodMask: String? = null,      // jcd — bitmask
    val minSeats: String? = null,
    val maxSeats: String? = null,
    val roomName: String? = null         // cdmc — search by name
)

/**
 * Query result wrapper with server-reported total count.
 */
data class RoomQueryResult(
    val rooms: List<EmptyRoom>,
    val totalCount: Int,
    val totalPage: Int
)

/**
 * Week/period bitmask utility.
 *
 * The教务系统 uses bitmasks:
 * - zcd: each bit position = week number (bit 0 = week 1)
 * - jcd: each bit position = period number (bit 0 = period 1)
 */
object BitmaskUtil {
    fun weekToMask(week: Int): String {
        return (1L shl (week - 1)).toString()
    }

    fun periodsToMask(periods: List<Int>): String {
        var mask = 0L
        for (p in periods) {
            mask = mask or (1L shl (p - 1))
        }
        return mask.toString()
    }

    fun maskToWeek(mask: String): Int? {
        val value = mask.toLongOrNull() ?: return null
        for (i in 0..20) {
            if ((1L shl i) == value) return i + 1
        }
        return null
    }
}
