package com.myhebnu.ui.grade.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.myhebnu.domain.GpaStrategy
import com.myhebnu.domain.SemesterGrades

@Composable
fun GradeTrendChart(
    semesters: List<SemesterGrades>,
    currentStrategy: GpaStrategy,
    modifier: Modifier = Modifier
) {
    if (semesters.size < 2) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "需至少两个学期的成绩才能显示趋势",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val sorted = semesters.sortedBy { it.semesterName }
    val values = sorted.map { sem ->
        when (currentStrategy) {
            GpaStrategy.SCALE_4_0 -> sem.gpa4
            GpaStrategy.SCALE_5_0 -> sem.gpa5
            GpaStrategy.WEIGHTED_PERCENTAGE -> sem.weightedAvg
        }
    }
    val labels = sorted.map { it.semesterName }
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "成绩趋势",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(start = 40.dp, end = 16.dp, top = 16.dp, bottom = 40.dp)
        ) {
            val w = size.width
            val h = size.height
            val maxVal = (values.maxOrNull() ?: 1f) * 1.1f
            val minVal = (values.minOrNull() ?: 0f) * 0.9f
            val range = (maxVal - minVal).coerceAtLeast(0.01f)

            // Grid lines
            val gridPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#E0E0E0")
                textSize = 24f
                isAntiAlias = true
            }

            // Y-axis labels
            for (i in 0..4) {
                val y = h * i / 4
                val value = maxVal - range * i / 4
                drawContext.canvas.nativeCanvas.drawText(
                    "%.1f".format(value), 0f, y + 8f, gridPaint
                )
                // Light grid line
                drawLine(
                    color = primaryColor.copy(alpha = 0.1f),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1f
                )
            }

            if (values.size < 2) return@Canvas

            // Line path
            val path = Path()
            val points = values.mapIndexed { index, value ->
                val x = w * index / (values.size - 1)
                val y = h * (1 - (value - minVal) / range)
                Offset(x, y)
            }

            path.moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size) {
                path.lineTo(points[i].x, points[i].y)
            }

            // Draw line
            drawPath(
                path = path,
                color = primaryColor,
                style = Stroke(width = 3.dp.toPx())
            )

            // Draw dots and labels
            points.forEachIndexed { index, point ->
                // Dot
                drawCircle(
                    color = primaryColor,
                    radius = 5.dp.toPx(),
                    center = point
                )
                drawCircle(
                    color = MaterialTheme.colorScheme.surface,
                    radius = 3.dp.toPx(),
                    center = point
                )

                // X-axis label
                val label = labels.getOrElse(index) { "" }
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    point.x - 20f,
                    h + 32f,
                    gridPaint
                )
            }
        }
    }
}
