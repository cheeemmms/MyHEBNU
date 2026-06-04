package com.myhebnu.ui.grade.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.myhebnu.domain.GpaStrategy
import com.myhebnu.domain.SemesterGrades
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.CartesianChartHost
import com.patrykandpatrick.vico.compose.chart.rememberCartesianChart
import com.patrykandpatrick.vico.compose.chart.scroll.rememberVicoScrollState
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries

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

    val semestersSorted = semesters.sortedBy { it.semesterName }

    // Build chart data
    val modelProducer = remember(semesters, currentStrategy) {
        CartesianChartModelProducer.build {
            lineSeries {
                series(semestersSorted.map { sem ->
                    when (currentStrategy) {
                        GpaStrategy.SCALE_4_0 -> sem.gpa4
                        GpaStrategy.SCALE_5_0 -> sem.gpa5
                        GpaStrategy.WEIGHTED_PERCENTAGE -> sem.weightedAvg
                    }
                })
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "成绩趋势",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberStartAxis(),
                rememberBottomAxis()
            ),
            modelProducer = modelProducer,
            scrollState = rememberVicoScrollState(),
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .padding(16.dp)
        )
    }
}
