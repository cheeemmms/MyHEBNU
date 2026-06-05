package com.myhebnu.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Grade
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.ui.graphics.vector.ImageVector
import com.myhebnu.R

/**
 * Top-level navigation destinations for the Navigation Drawer.
 */
sealed class TopLevelRoute(
    val route: String,
    val titleResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Schedule : TopLevelRoute(
        route = "schedule",
        titleResId = R.string.nav_schedule,
        selectedIcon = Icons.Filled.CalendarToday,
        unselectedIcon = Icons.Outlined.CalendarToday
    )

    data object Grade : TopLevelRoute(
        route = "grade",
        titleResId = R.string.nav_grade,
        selectedIcon = Icons.Filled.Grade,
        unselectedIcon = Icons.Outlined.Grade
    )

    data object EmptyRoom : TopLevelRoute(
        route = "empty_room",
        titleResId = R.string.nav_empty_room,
        selectedIcon = Icons.Filled.MeetingRoom,
        unselectedIcon = Icons.Outlined.MeetingRoom
    )

    data object Exam : TopLevelRoute(
        route = "exam",
        titleResId = R.string.nav_exam,
        selectedIcon = Icons.AutoMirrored.Filled.Assignment,
        unselectedIcon = Icons.AutoMirrored.Outlined.Assignment
    )
}

val topLevelRoutes = listOf(
    TopLevelRoute.Schedule,
    TopLevelRoute.Grade,
    TopLevelRoute.EmptyRoom,
    TopLevelRoute.Exam
)
