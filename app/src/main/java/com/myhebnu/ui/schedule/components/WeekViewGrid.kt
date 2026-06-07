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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.myhebnu.data.local.db.entity.CourseEntity
import com.myhebnu.ui.schedule.PeriodInfo
import com.myhebnu.ui.theme.CourseTonalPalette
import com.myhebnu.ui.theme.coursePaletteForHue

/**
 * Two-layer architecture:
 *   Layer 1 (bottom) — empty grid: header + period rows (lines & labels only)
 *   Layer 2 (top)    — course cards: absolute positioning via [offset], unconstrained by row height
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
    val totalPeriods = periodLabels.size
    val headerHeight = 32.dp

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val cellWidth: Dp = (maxWidth - timeColumnWidth) / columns
        val rowHeight: Dp = (maxHeight - headerHeight) / totalPeriods

        // ═══ Layer 1: Empty grid (lines + labels only, no course cards) ═══
        Column(modifier = Modifier.fillMaxSize()) {
            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerHeight)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(vertical = 4.dp)
            ) {
                Box(Modifier.width(timeColumnWidth), contentAlignment = Alignment.Center) {
                    Text("节", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                dayLabels.forEachIndexed { idx, label ->
                    val isToday = (displayWeek == currentWeek) && (idx + 1 == todayDayOfWeek)
                    Box(
                        modifier = Modifier
                            .width(cellWidth)
                            .then(if (isToday) Modifier.clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer) else Modifier)
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("周$label", style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (isToday) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Period rows — empty cells with grid lines only
            for (periodIdx in 0 until totalPeriods) {
                Row(modifier = Modifier.fillMaxWidth().height(rowHeight)) {
                    Box(
                        modifier = Modifier
                            .width(timeColumnWidth).fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .border(0.5.dp, gridLineColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(periodLabels[periodIdx].startPeriod.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium, fontSize = 11.sp)
                            Text(periodLabels[periodIdx].startTime,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    for (dayIdx in 0 until columns) {
                        val isToday = (displayWeek == currentWeek) && (dayIdx + 1 == todayDayOfWeek)
                        Box(
                            modifier = Modifier
                                .width(cellWidth).fillMaxHeight()
                                .border(0.5.dp, gridLineColor)
                                .then(if (isToday) Modifier.background(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.04f)
                                ) else Modifier)
                        )
                    }
                }
            }
        }

        // ═══ Layer 2: Course cards (absolute positioning, unconstrained by row height) ═══
        Box(modifier = Modifier.fillMaxSize()) {
            for (course in courses) {
                val dayIdx = course.dayOfWeek - 1
                if (dayIdx !in 0 until columns) continue

                val periodIdx = periodLabels.indexOfFirst { it.startPeriod == course.startPeriod }
                if (periodIdx < 0) continue

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
                        .offset(x = timeColumnWidth + cellWidth * dayIdx,
                            y = headerHeight + rowHeight * periodIdx)
                        .width(cellWidth)
                        .height(rowHeight * spanCount)
                        .zIndex(course.startPeriod.toFloat())
                )
            }
        }
    }
}
