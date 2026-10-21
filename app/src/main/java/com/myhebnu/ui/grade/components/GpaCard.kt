package com.myhebnu.ui.grade.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.myhebnu.R
import com.myhebnu.domain.GpaStrategy

/**
 * GPA 主卡。
 *
 * 三种算法互斥，按 MD3 用 `SegmentedButton`（单选分段按钮）而不是 `FilterChip`
 * ——chip 的语义是「多选筛选 / 标签」，用在单选上会让用户误以为可以多选。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GpaCard(
    currentStrategy: GpaStrategy,
    currentGpa: Float,
    totalCredits: Float,
    onStrategyChange: (GpaStrategy) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.Start
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                GpaStrategy.entries.forEachIndexed { index, strategy ->
                    SegmentedButton(
                        selected = currentStrategy == strategy,
                        onClick = { onStrategyChange(strategy) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = GpaStrategy.entries.size
                        ),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            activeContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            activeBorderColor = Color.Transparent,
                            inactiveContainerColor = Color.Transparent,
                            inactiveContentColor = MaterialTheme.colorScheme.onSurface,
                            inactiveBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        label = {
                            Text(
                                text = strategy.label,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = formatGpa(currentStrategy, currentGpa),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = getStrategyLabel(currentStrategy),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.total_credits) + ": ${"%.1f".format(totalCredits)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatGpa(strategy: GpaStrategy, value: Float): String {
    return when (strategy) {
        GpaStrategy.SCALE_4_0 -> "%.2f".format(value)
        GpaStrategy.SCALE_5_0 -> "%.2f".format(value)
        GpaStrategy.WEIGHTED_PERCENTAGE -> "%.1f".format(value)
    }
}

private fun getStrategyLabel(strategy: GpaStrategy): String {
    return when (strategy) {
        GpaStrategy.SCALE_4_0 -> "GPA (4.0)"
        GpaStrategy.SCALE_5_0 -> "GPA (5.0)"
        GpaStrategy.WEIGHTED_PERCENTAGE -> "加权平均分"
    }
}
