package com.myhebnu.ui.schedule.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
 * Two-layer architecture with fixed header + scrollable grid:
 *   Header row — day labels, always visible (fixed, non-scrolling)
 *   Grid area  — verticalScroll container with Layer 1 (grid lines) + Layer 2 (cards)
 *
 * rowHeight is fixed (55.dp) so all 13 periods are rendered at a readable size;
 * the grid scrolls vertically when it exceeds the available viewport.
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
    val rowHeight = 55.dp // Fixed per-period height — scrolls when content overflows
    val isDark = isSystemInDarkTheme()
    val scrollState = rememberScrollState()

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val cellWidth: Dp = (maxWidth - timeColumnWidth) / columns

        Column(modifier = Modifier.fillMaxSize()) {
            // ═══════════════════════════════════════════
            // Fixed header row (day labels — "周一" … "周五")
            // ═══════════════════════════════════════════
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerHeight)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(vertical = 4.dp)
            ) {
                Box(Modifier.width(timeColumnWidth), contentAlignment = Alignment.Center) {
                    Text(
                        "节", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                dayLabels.forEachIndexed { idx, label ->
                    val isToday = (displayWeek == currentWeek) && (idx + 1 == todayDayOfWeek)
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
                            "周$label", style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (isToday) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ═══════════════════════════════════════════
            // Scrollable grid area (Layer 1 grid + Layer 2 cards)
            // ═══════════════════════════════════════════
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState)
            ) {
                // Explicit total height so compose knows the scroll extent
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(rowHeight * totalPeriods)
                ) {
                    // --- Layer 1: Grid lines + labels ---
                    Column {
                        for (periodIdx in 0 until totalPeriods) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(rowHeight)
                            ) {
                                // Time label cell
                                Box(
                                    modifier = Modifier
                                        .width(timeColumnWidth).fillMaxHeight()
                                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                        .border(0.5.dp, gridLineColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            periodLabels[periodIdx].startPeriod.toString(),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Medium, fontSize = 11.sp
                                        )
                                        Text(
                                            periodLabels[periodIdx].startTime,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Day cells (empty grid)
                                for (dayIdx in 0 until columns) {
                                    val isToday =
                                        (displayWeek == currentWeek) && (dayIdx + 1 == todayDayOfWeek)
                                    Box(
                                        modifier = Modifier
                                            .width(cellWidth).fillMaxHeight()
                                            .border(0.5.dp, gridLineColor)
                                            .then(
                                                if (isToday) Modifier.background(
                                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.04f)
                                                ) else Modifier
                                            )
                                    )
                                }
                            }
                        }
                    }

                    // --- Layer 2: Course cards (absolute positioning via offset) ---
                    for (course in courses) {
                        val dayIdx = course.dayOfWeek - 1
                        if (dayIdx !in 0 until columns) continue

                        val periodIdx =
                            periodLabels.indexOfFirst { it.startPeriod == course.startPeriod }
                        if (periodIdx < 0) continue

                        val spanCount = (course.endPeriod - course.startPeriod + 1)
                            .coerceAtMost(totalPeriods - periodIdx)
                        val palette = coursePalettes[course.courseName]
                            ?: coursePaletteForHue(course.color.toFloat(), isDark)

                        CourseCard(
                            course = course,
                            isActive = course.id == activeCourseId,
                            palette = palette,
                            onClick = { onCourseClick(course) },
                            modifier = Modifier
                                .offset(
                                    x = timeColumnWidth + cellWidth * dayIdx + 2.dp,
                                    y = rowHeight * periodIdx + 2.dp
                                )
                                .width(cellWidth - 4.dp)
                                .height(rowHeight * spanCount - 4.dp)
                                .zIndex(course.startPeriod.toFloat())
                        )
                    }
                }
            }
        }
    }
}
