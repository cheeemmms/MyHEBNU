package com.myhebnu.ui.grade.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myhebnu.R
import com.myhebnu.domain.GpaStrategy

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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Strategy selector chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GpaStrategy.entries.forEach { strategy ->
                    FilterChip(
                        selected = currentStrategy == strategy,
                        onClick = { onStrategyChange(strategy) },
                        label = {
                            Text(
                                text = strategy.label,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // GPA value
            Text(
                text = formatGpa(currentStrategy, currentGpa),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 36.sp
                ),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Text(
                text = getStrategyLabel(currentStrategy),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.total_credits) + ": ${"%.1f".format(totalCredits)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
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
