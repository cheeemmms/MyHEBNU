package com.myhebnu.ui.room.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.myhebnu.R
import com.myhebnu.domain.Building

/**
 * 星期标签 (周一~周日)
 */
private val DAY_LABELS = listOf("一", "二", "三", "四", "五", "六", "日")

@Composable
fun FilterPanel(
    campuses: List<Building>,
    selectedCampusId: String,
    onCampusChange: (String) -> Unit,
    buildings: List<Building>,
    selectedBuilding: Building?,
    onBuildingChange: (Building?) -> Unit,
    selectedWeek: Int,
    onWeekChange: (Int) -> Unit,
    selectedDays: Set<Int>,
    onDayToggle: (Int) -> Unit,
    selectedPeriods: Set<Int>,
    onPeriodToggle: (Int) -> Unit,
    isLoading: Boolean,
    onQuery: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1: Campus + Building
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Campus dropdown
                FilterDropdown(
                    label = stringResource(R.string.room_campus),
                    value = campuses.find { it.code == selectedCampusId }?.name ?: "",
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                ) {
                    campuses.forEach { campus ->
                        DropdownMenuItem(
                            text = { Text(campus.name) },
                            onClick = { onCampusChange(campus.code) }
                        )
                    }
                }

                // Building dropdown
                FilterDropdown(
                    label = stringResource(R.string.room_building),
                    value = selectedBuilding?.name ?: "全部",
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                ) {
                    DropdownMenuItem(
                        text = { Text("全部") },
                        onClick = { onBuildingChange(null) }
                    )
                    buildings.forEach { building ->
                        DropdownMenuItem(
                            text = { Text(building.name) },
                            onClick = { onBuildingChange(building) }
                        )
                    }
                }
            }

            // Row 2: Week selector
            WeekChipSelector(
                label = stringResource(R.string.room_week),
                selected = selectedWeek,
                range = 1..20,
                onSelect = onWeekChange,
                enabled = !isLoading
            )

            // Row 3: Day of week (multi-select)
            ChipGroup(
                label = stringResource(R.string.room_day),
                items = DAY_LABELS.mapIndexed { i, label ->
                    ChipItem(index = i + 1, label = "周$label", selected = (i + 1) in selectedDays)
                },
                onToggle = onDayToggle,
                enabled = !isLoading
            )

            // Row 4: Period selector (multi-select)
            PeriodChipSelector(
                selected = selectedPeriods,
                onToggle = onPeriodToggle,
                enabled = !isLoading
            )

            // Query button
            Button(
                onClick = onQuery,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.room_query))
            }
        }
    }
}

@Composable
private fun FilterDropdown(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box {
            OutlinedButton(
                onClick = { if (enabled) expanded = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled
            ) {
                Text(
                    text = value,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                content()
            }
        }
    }
}

@Composable
private fun WeekChipSelector(
    label: String,
    selected: Int,
    range: IntRange,
    onSelect: (Int) -> Unit,
    enabled: Boolean
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedButton(
            onClick = { if (enabled) expanded = true },
            enabled = enabled
        ) {
            Text("第 $selected 周")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            range.forEach { week ->
                DropdownMenuItem(
                    text = {
                        Text(
                            "第 $week 周",
                            fontWeight = if (week == selected) {
                                androidx.compose.ui.text.font.FontWeight.Bold
                            } else {
                                androidx.compose.ui.text.font.FontWeight.Normal
                            }
                        )
                    },
                    onClick = {
                        onSelect(week)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ChipGroup(
    label: String,
    items: List<ChipItem>,
    onToggle: (Int) -> Unit,
    enabled: Boolean
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items.forEach { item ->
                FilterChip(
                    selected = item.selected,
                    onClick = { if (enabled) onToggle(item.index) },
                    label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                    enabled = enabled
                )
            }
        }
    }
}

data class ChipItem(
    val index: Int,
    val label: String,
    val selected: Boolean
)

@Composable
private fun PeriodChipSelector(
    selected: Set<Int>,
    onToggle: (Int) -> Unit,
    enabled: Boolean
) {
    Column {
        Text(
            text = stringResource(R.string.room_period),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Common period groups: 1-2, 3-4, 5-6, 7-8, 9-10, 11-13
            val groups = listOf(
                "1-2节" to listOf(1, 2),
                "3-4节" to listOf(3, 4),
                "5-6节" to listOf(5, 6),
                "7-8节" to listOf(7, 8),
                "9-10节" to listOf(9, 10),
                "11-13节" to listOf(11, 12, 13)
            )
            groups.forEach { (label, periods) ->
                val allSelected = periods.all { it in selected }
                FilterChip(
                    selected = allSelected,
                    onClick = {
                        if (enabled) periods.forEach(onToggle)
                    },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    enabled = enabled
                )
            }
        }
    }
}
