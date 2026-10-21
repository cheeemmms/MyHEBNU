package com.myhebnu.ui.room.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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

/**
 * 空教室筛选面板。
 *
 * 交互全部使用 MD3 原生组件：下拉用 `ExposedDropdownMenuBox` + `OutlinedTextField`（只读），
 * 多选筛选用 `FilterChip`，横向 chip 列表用 `LazyRow`（避免最后一项被屏幕边缘截断）。
 */
@Composable
fun FilterPanel(
    semesterYear: String,
    onYearChange: (String) -> Unit,
    semesterTerm: String,
    onTermChange: (String) -> Unit,
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
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Row 1: Year + Semester
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val yearOptions = listOf(
                    "2024" to "2024-2025",
                    "2025" to "2025-2026",
                    "2026" to "2026-2027"
                )
                FilterDropdown(
                    label = "学年",
                    value = yearOptions.find { it.first == semesterYear }?.second ?: semesterYear,
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                ) { closeMenu ->
                    yearOptions.forEach { (code, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                onYearChange(code)
                                closeMenu()
                            }
                        )
                    }
                }

                val termOptions = listOf(
                    "3" to "第1学期",
                    "12" to "第2学期",
                    "16" to "暑假"
                )
                FilterDropdown(
                    label = "学期",
                    value = termOptions.find { it.first == semesterTerm }?.second ?: semesterTerm,
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                ) { closeMenu ->
                    termOptions.forEach { (code, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                onTermChange(code)
                                closeMenu()
                            }
                        )
                    }
                }
            }

            // Row 2: Campus + Building
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterDropdown(
                    label = stringResource(R.string.room_campus),
                    value = campuses.find { it.code == selectedCampusId }?.name ?: "",
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                ) { closeMenu ->
                    campuses.forEach { campus ->
                        DropdownMenuItem(
                            text = { Text(campus.name) },
                            onClick = {
                                onCampusChange(campus.code)
                                closeMenu()
                            }
                        )
                    }
                }

                FilterDropdown(
                    label = stringResource(R.string.room_building),
                    value = selectedBuilding?.name ?: "全部",
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                ) { closeMenu ->
                    DropdownMenuItem(
                        text = { Text("全部") },
                        onClick = {
                            onBuildingChange(null)
                            closeMenu()
                        }
                    )
                    buildings.forEach { building ->
                        DropdownMenuItem(
                            text = { Text(building.name) },
                            onClick = {
                                onBuildingChange(building)
                                closeMenu()
                            }
                        )
                    }
                }
            }

            // Row 3: Week selector
            FilterDropdown(
                label = stringResource(R.string.room_week),
                value = "第 $selectedWeek 周",
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) { closeMenu ->
                (1..20).forEach { week ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "第 $week 周",
                                fontWeight = if (week == selectedWeek) {
                                    androidx.compose.ui.text.font.FontWeight.Medium
                                } else {
                                    androidx.compose.ui.text.font.FontWeight.Normal
                                }
                            )
                        },
                        onClick = {
                            onWeekChange(week)
                            closeMenu()
                        }
                    )
                }
            }

            // Row 4: Day of week (multi-select)
            ChipGroup(
                label = stringResource(R.string.room_day),
                items = DAY_LABELS.mapIndexed { i, label ->
                    ChipItem(index = i + 1, label = "周$label", selected = (i + 1) in selectedDays)
                },
                onToggle = onDayToggle,
                enabled = !isLoading
            )

            // Row 5: Period selector (multi-select)
            PeriodChipSelector(
                selected = selectedPeriods,
                onToggle = onPeriodToggle,
                enabled = !isLoading
            )

            // Query button
            Button(
                onClick = onQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                enabled = !isLoading
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
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.room_query), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/**
 * MD3 暴露式下拉：只读 `OutlinedTextField` + 下拉菜单。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterDropdown(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable ColumnScope.(closeMenu: () -> Unit) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            enabled = enabled,
            // 值用 bodyMedium：两列窄栏下 bodyLarge 会把「2026-2027」挤掉尾部
            textStyle = MaterialTheme.typography.bodyMedium,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            content { expanded = false }
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
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items, key = { it.index }) { item ->
                FilterChip(
                    selected = item.selected,
                    onClick = { if (enabled) onToggle(item.index) },
                    label = { Text(item.label) },
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
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 常用节次分组：1-2、3-4、5-6、7-8、9-10、11-13
            val groups = listOf(
                "1-2节" to listOf(1, 2),
                "3-4节" to listOf(3, 4),
                "5-6节" to listOf(5, 6),
                "7-8节" to listOf(7, 8),
                "9-10节" to listOf(9, 10),
                "11-13节" to listOf(11, 12, 13)
            )
            items(groups) { (label, periods) ->
                val allSelected = periods.all { it in selected }
                FilterChip(
                    selected = allSelected,
                    onClick = { if (enabled) periods.forEach(onToggle) },
                    label = { Text(label) },
                    enabled = enabled
                )
            }
        }
    }
}
