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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myhebnu.data.local.db.entity.CourseEntity
import com.myhebnu.ui.schedule.PeriodInfo
import com.myhebnu.ui.theme.CourseTonalPalette
import com.myhebnu.ui.theme.coursePaletteForHue

/**
 * Per-period grid: each period (1,2,3...) is one row.
 * Courses only fill the periods they actually occupy.
 * Week selector moved to parent's bottom area.
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
    coursePalettes: Map<String, CourseTonalPalette>,
    onCourseClick: (CourseEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val columns = dayLabels.size
    val timeColumnWidth = 40.dp
    val gridLineAlpha = 0.2f
    val gridLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = gridLineAlpha)

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val cellWidth = (maxWidth - timeColumnWidth) / columns
        val totalPeriods = periodLabels.size

        Column(modifier = Modifier.fillMaxSize()) {
            // --- Header row ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(vertical = 4.dp)
            ) {
                // Time column header
                Box(
                    modifier = Modifier.width(timeColumnWidth),
                    contentAlignment = Alignment.Center
                ) {
                    Text("节", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // Day headers
                dayLabels.forEachIndexed { index, label ->
                    val isToday = (displayWeek == currentWeek) && (index + 1 == todayDayOfWeek)
                    Box(
                        modifier = Modifier
                            .width(cellWidth)
                            .then(
                                if (isToday) Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                else Modifier
                            )
                            .padding(vertical = 4.dp),
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

            // --- Period rows ---
            // Build a map: (periodIndex, dayIndex) -> course(s)
            val startPeriods = periodLabels.map { it.startPeriod }

            for (periodIdx in 0 until totalPeriods) {
                val period = startPeriods[periodIdx]
                val pi = periodLabels[periodIdx]

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // Period label — show period number + start time
                    Box(
                        modifier = Modifier
                            .width(timeColumnWidth)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .border(0.5.dp, gridLineColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = period.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp
                            )
                            Text(
                                text = pi.startTime,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Day cells
                    for (dayIdx in 0 until columns) {
                        val isToday = (displayWeek == currentWeek) && (dayIdx + 1 == todayDayOfWeek)
                        val cellCourses = courses.filter { c ->
                            c.dayOfWeek == dayIdx + 1 &&
                            c.startPeriod == period
                        }

                        Box(
                            modifier = Modifier
                                .width(cellWidth)
                                .fillMaxHeight()
                                .border(0.5.dp, gridLineColor)
                                .then(
                                    if (isToday) Modifier.background(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.04f)
                                    ) else Modifier
                                ),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            if (cellCourses.isNotEmpty()) {
                                val course = cellCourses.first()
                                val spanCount = (course.endPeriod - course.startPeriod + 1)
                                    .coerceAtMost(totalPeriods - periodIdx)
                                val palette = coursePalettes[course.courseName]
                                    ?: coursePaletteForHue(course.color.toFloat(), false)

                                CourseCard(
                                    course = course,
                                    isActive = course.id == activeCourseId,
                                    palette = palette,
                                    onClick = { onCourseClick(course) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(spanCount.toFloat() / (totalPeriods - periodIdx).toFloat())
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
