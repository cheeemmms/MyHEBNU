package com.myhebnu.ui.schedule.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
 * 5-column (Mon-Fri) week-view grid with dynamic column widths.
 * No horizontal scroll — fills screen width. Rows fill available height equally.
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
    onCourseClick: (CourseEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val columns = dayLabels.size // 5 (Mon-Fri)
    val timeColumnWidth = 48.dp

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val availableWidth = maxWidth - timeColumnWidth
        val columnWidth = availableWidth / columns

        val courseGrid = buildCourseGrid(courses, periodLabels, columns)
        val gridRows = periodLabels.size

        Column(modifier = Modifier.fillMaxSize()) {
            // Header row: time corner + day labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(vertical = 4.dp)
            ) {
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
                dayLabels.forEachIndexed { index, label ->
                    val isToday = (displayWeek == currentWeek) && (index + 1 == todayDayOfWeek)
                    Box(
                        modifier = Modifier
                            .width(columnWidth)
                            .then(
                                if (isToday) Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                else Modifier
                            )
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "周$label",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (isToday) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Period rows — equal height, fill remaining space
            periodLabels.forEachIndexed { periodIndex, periodInfo ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)  // Equal share of remaining height
                ) {
                    // Compact 3-line period label
                    PeriodLabel(
                        periodInfo = periodInfo,
                        modifier = Modifier.width(timeColumnWidth)
                    )

                    // Day cells
                    for (dayIndex in 0 until columns) {
                        val cellCourses = courseGrid[periodIndex][dayIndex]
                        val isToday = (displayWeek == currentWeek) && (dayIndex + 1 == todayDayOfWeek)

                        Box(
                            modifier = Modifier
                                .width(columnWidth)
                                .fillMaxHeight()
                                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                .then(
                                    if (isToday) Modifier.background(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.05f)
                                    ) else Modifier
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
                                            onClick = { onCourseClick(course) },
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
}

/**
 * Compact 3-line period label:
 *   1
 * 08:00
 * 09:40
 */
@Composable
private fun PeriodLabel(periodInfo: PeriodInfo, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = periodInfo.startPeriod.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp
            )
            Text(
                text = periodInfo.startTime,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = periodInfo.endTime,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

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

        var placed = false
        for (rowIdx in 0 until rows) {
            val pi = periodLabels[rowIdx]
            if (course.startPeriod <= pi.endPeriod && course.endPeriod >= pi.startPeriod) {
                grid[rowIdx][dayIdx] = grid[rowIdx][dayIdx] + course
                placed = true
                break
            }
        }
        if (!placed && rows > 0) {
            var bestRow = 0
            var bestDist = Int.MAX_VALUE
            for (rowIdx in 0 until rows) {
                val dist = kotlin.math.abs(course.startPeriod - periodLabels[rowIdx].startPeriod)
                if (dist < bestDist) { bestDist = dist; bestRow = rowIdx }
            }
            grid[bestRow][dayIdx] = grid[bestRow][dayIdx] + course
        }
    }
    return grid
}
