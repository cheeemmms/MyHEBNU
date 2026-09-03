package com.myhebnu.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.myhebnu.R
import java.time.LocalDate

/** 路由入口：从首页横幅进入。 */
@Composable
fun SemesterSetupScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    SemesterSetupBottomSheet(
        onDismiss = onBack,
        onSaved = onBack,
        modifier = modifier
    )
}

/**
 * 学期设置底部弹层。
 * 复用于：1) 首页横幅进入；2) 设置页「当前学期」点击进入。
 * 交互采用安卓原生 Material 3 控件：学年用 ExposedDropdownMenuBox 下拉，学期用 SingleChoiceSegmentedButtonRow 分段按钮。
 * 用户只需选择学年 + 学期，保存时自动从 N2154 拉取周次映射推导开学日/放假日。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SemesterSetupBottomSheet(
    onDismiss: () -> Unit,
    onSaved: () -> Unit = onDismiss,
    modifier: Modifier = Modifier,
    viewModel: SemesterSetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val now = LocalDate.now()
    val currentAcademicStart = if (now.monthValue >= 9) now.year else now.year - 1
    // 学年选项：前后各 5 年，确保能覆盖在校生常用范围。
    val yearOptions = remember(currentAcademicStart) {
        ((currentAcademicStart - 5)..(currentAcademicStart + 4)).map { y -> "$y-${y + 1}" }
    }
    val termOptions = listOf(
        stringResource(R.string.term_first) to "3",
        stringResource(R.string.term_second) to "12",
        stringResource(R.string.term_third) to "16"
    )

    val selectedYearIndex = remember(uiState.year, yearOptions) {
        yearOptions.indexOfFirst { it.startsWith("${uiState.year}-") }.coerceAtLeast(0)
    }
    val selectedTermIndex = remember(uiState.term, termOptions) {
        termOptions.indexOfFirst { it.second == uiState.term }.coerceAtLeast(0)
    }

    var yearExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.semester_setup_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(24.dp))

            // 学年：下拉选择（Material 3 原生，复用与设置页主题选择一致的模式）
            ExposedDropdownMenuBox(
                expanded = yearExpanded,
                onExpandedChange = { yearExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .clickable { yearExpanded = true },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.semester_setup_year),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = yearOptions.getOrElse(selectedYearIndex) { "" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                ExposedDropdownMenu(
                    expanded = yearExpanded,
                    onDismissRequest = { yearExpanded = false }
                ) {
                    yearOptions.forEachIndexed { index, option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                val startYear = currentAcademicStart - 5 + index
                                viewModel.setYear(startYear.toString())
                                yearExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // 学期：分段按钮（固定 3 项，Material 3 单选区按钮）
            Text(
                stringResource(R.string.semester_setup_term),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                termOptions.forEachIndexed { index, (label, value) ->
                    SegmentedButton(
                        selected = selectedTermIndex == index,
                        onClick = { viewModel.setTerm(value) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = termOptions.size),
                        label = { Text(label) }
                    )
                }
            }

            if (uiState.error != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = onDismiss,
                    enabled = !uiState.isSaving
                ) {
                    Text(stringResource(R.string.cancel))
                }

                Button(
                    onClick = { viewModel.save { onSaved() } },
                    enabled = !uiState.isSaving
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(stringResource(R.string.semester_setup_save))
                    }
                }
            }
        }
    }
}
