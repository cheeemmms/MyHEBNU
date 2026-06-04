package com.myhebnu.ui.schedule.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myhebnu.data.local.db.entity.CourseEntity
import com.myhebnu.ui.schedule.PeriodInfo

/**
 * The week-view grid showing courses as colored cards arranged
 * in a grid: 7 columns (Mon-Sun) × 6 period rows.
 *
 * Horizontally scrollable for smaller screens.
 */
@Composable
fun WeekViewGrid(
    courses: List<CourseEntity>,
    dayLabels: List<String>,
    periodLabels: List<PeriodInfo>,
    displayWeek: Int,
    currentWeek: Int,
    todayDayOfWeek: Int,
    activeCourseId: String?,
    modifier: Modifier = Modifier
) {
    val horizontalScrollState = rememberScrollState()
    val columns = dayLabels.size // 7
    val columnWidth = 100.dp
    val timeColumnWidth = 50.dp
    val rowMinHeight = 70.dp

    // Group courses by (dayOfWeek, period index)
    val courseGrid = buildCourseGrid(courses, periodLabels, columns)

    Column(modifier = modifier) {
        // Header row: empty corner + day labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(horizontalScrollState)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(vertical = 4.dp)
        ) {
            // Time label corner
            Box(
                modifier = Modifier.width(timeColumnWidth),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "节次",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Day headers
            dayLabels.forEachIndexed { index, label ->
                val isToday = (displayWeek == currentWeek) && (index + 1 == todayDayOfWeek)
                Box(
                    modifier = Modifier
                        .width(columnWidth)
                        .then(
                            if (isToday) {
                                Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                            } else Modifier
                        )
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "周$label",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                        color = if (isToday) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }

        // Period rows
        periodLabels.forEachIndexed { periodIndex, periodInfo ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(horizontalScrollState)
                    .height(rowMinHeight)
            ) {
                // Period label (time column)
                Box(
                    modifier = Modifier
                        .width(timeColumnWidth)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .border(
                            0.5.dp,
                            MaterialTheme.colorScheme.outlineVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = periodInfo.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp
                        )
                        Text(
                            text = periodInfo.timeRange,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Day cells for this period row
                for (dayIndex in 0 until columns) {
                    val cellCourses = courseGrid[periodIndex][dayIndex]
                    val isToday = (displayWeek == currentWeek) && (dayIndex + 1 == todayDayOfWeek)

                    Box(
                        modifier = Modifier
                            .width(columnWidth)
                            .fillMaxHeight()
                            .border(
                                0.5.dp,
                                MaterialTheme.colorScheme.outlineVariant
                            )
                            .then(
                                if (isToday) {
                                    Modifier.background(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.05f)
                                    )
                                } else Modifier
                            ),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        if (cellCourses.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight()
                                    .padding(1.dp)
                            ) {
                                cellCourses.forEach { course ->
                                    CourseCard(
                                        course = course,
                                        isActive = course.id == activeCourseId,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Build a 2D grid: [periodRows][dayColumns] → List<CourseEntity>
 * Assigns each course to the correct period row(s) based on its period range.
 */
private fun buildCourseGrid(
    courses: List<CourseEntity>,
    periodLabels: List<PeriodInfo>,
    columns: Int
): Array<Array<List<CourseEntity>>> {
    val rows = periodLabels.size
    val grid = Array(rows) { Array(columns) { emptyList<CourseEntity>() } }

    for (course in courses) {
        val dayIdx = course.dayOfWeek - 1
        if (dayIdx !in 0 until columns) continue

        // Find which period group this course belongs to
        // A course "3-5" overlaps with period group "3-4"
        // We place it in the first matching group
        var placed = false
        for (rowIdx in 0 until rows) {
            val pi = periodLabels[rowIdx]
            // Check if the course overlaps with this period group
            if (course.startPeriod <= pi.endPeriod && course.endPeriod >= pi.startPeriod) {
                grid[rowIdx][dayIdx] = grid[rowIdx][dayIdx] + course
                placed = true
                break // Place in first matching group
            }
        }
        // Fallback: place in closest group
        if (!placed && rows > 0) {
            // Find the closest group based on startPeriod
            var bestRow = 0
            var bestDist = Int.MAX_VALUE
            for (rowIdx in 0 until rows) {
                val dist = kotlin.math.abs(course.startPeriod - periodLabels[rowIdx].startPeriod)
                if (dist < bestDist) {
                    bestDist = dist
                    bestRow = rowIdx
                }
            }
            grid[bestRow][dayIdx] = grid[bestRow][dayIdx] + course
        }
    }

    return grid
}
