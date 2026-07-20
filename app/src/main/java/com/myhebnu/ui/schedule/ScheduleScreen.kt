package com.myhebnu.ui.schedule

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.myhebnu.R
import com.myhebnu.ui.schedule.components.CourseDetailSheet
import com.myhebnu.ui.schedule.components.WeekSelector
import com.myhebnu.ui.schedule.components.WeekViewGrid
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    modifier: Modifier = Modifier,
    viewModel: ScheduleViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    viewModel.updateSystemDarkMode(isSystemInDarkTheme())  // sync system dark mode for course card palettes
    val today = LocalDate.now()
    val todayDayOfWeek = today.dayOfWeek.value // Mon=1 ... Sun=7
    val snackbarHostState = remember { SnackbarHostState() }

    // Show error in snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    // #29 进入课表页：重置「暂时忽略」会话态并重算浮层（每次进页都重新评估）
    LaunchedEffect(Unit) {
        viewModel.onScheduleEntered()
    }

    // #29 下学期查询无课表提示（消费标记，避免重复弹）
    LaunchedEffect(uiState.nextSemesterUnavailable) {
        if (uiState.nextSemesterUnavailable) {
            snackbarHostState.showSnackbar("暂无新课表")
            viewModel.consumeNextSemesterUnavailable()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_schedule)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Content area (fills available space)
            Box(modifier = Modifier.weight(1f)) {
                when {
                    uiState.isLoading && !uiState.isCached -> {
                        // First load — show loading skeleton
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = stringResource(R.string.loading),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    uiState.error != null && !uiState.isCached -> {
                        // Error with no cache
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = uiState.error ?: stringResource(R.string.error_network),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(Modifier.height(16.dp))
                                Button(onClick = viewModel::refreshSchedule) {
                                    Text(stringResource(R.string.retry))
                                }
                            }
                        }
                    }
                    uiState.courses.isEmpty() && uiState.isCached -> {
                        // No courses for this semester
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.no_schedule),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    else -> {
                        // Show week view grid
                        WeekViewGrid(
                            courses = uiState.filteredCourses,
                            dayLabels = uiState.dayLabels,
                            periodLabels = uiState.periodLabels,
                            displayWeek = uiState.displayWeek,
                            currentWeek = uiState.currentWeek,
                            todayDayOfWeek = todayDayOfWeek,
                            activeCourseId = uiState.activeCourseId,
                            coursePalettes = uiState.coursePalettes,
                            onCourseClick = { course -> viewModel.selectCourse(course) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } // end when

                // #29 下学期课表浮层——三态复用同一 MD3 卡片（when 之后、Box 之内）
                if (uiState.panelMode != SemesterPanelMode.NONE) {
                    SemesterEndFloatingPanel(
                        mode = uiState.panelMode,
                        nextTermLabel = uiState.nextTermLabel,
                        isLoading = uiState.queryingNextSemester,
                        onDismiss = viewModel::dismissSemesterEndPanel,
                        onPrimary = {
                            when (uiState.panelMode) {
                                SemesterPanelMode.DISCOVERY -> viewModel.queryNextSemesterSchedule()
                                SemesterPanelMode.REENTRY -> viewModel.viewNextSemester()
                                SemesterPanelMode.BACK -> viewModel.viewCurrentSemester()
                                else -> {}
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 56.dp)
                    )
                }
            } // End Box(modifier.weight(1f))

            // Week selector at bottom
            WeekSelector(
                displayWeek = uiState.displayWeek,
                currentWeek = uiState.currentWeek,
                onPreviousWeek = viewModel::goToPreviousWeek,
                onNextWeek = viewModel::goToNextWeek,
                onGoToCurrentWeek = viewModel::goToCurrentWeek
            )
        }
    }

    // Course detail BottomSheet
    val selectedCourse = uiState.selectedCourse
    if (selectedCourse != null) {
        CourseDetailSheet(
            course = selectedCourse,
            onDismiss = { viewModel.selectCourse(null) }
        )
    }
}

/**
 * #29 下学期课表浮层——MD3 卡片式浮动层，三态复用同一套素材（保持视觉一致）。
 * 浮于课表网格下半区，不压暗背景。依据 [mode] 切换文案与按钮：
 *   - DISCOVERY：标题「本学期已结束」+ 副标题 + 暂时忽略 / 查询课表
 *   - REENTRY：  标题「下学期课表已公布，是否查看？」+ 暂时忽略 / 查看
 *   - BACK：     标题「正在查看下学期课表」+ 返回本学期（无忽略）
 */
private data class PanelCopy(
    val title: String,
    val subtitle: String,
    val actionLabel: String,
    val showDismiss: Boolean
)

private fun panelCopy(mode: SemesterPanelMode, nextTermLabel: String): PanelCopy = when (mode) {
    SemesterPanelMode.DISCOVERY -> PanelCopy(
        title = "本学期已结束",
        subtitle = "下学期（${nextTermLabel}）课表可能已经公布，是否查询？",
        actionLabel = "查询课表",
        showDismiss = true
    )
    SemesterPanelMode.REENTRY -> PanelCopy(
        title = "下学期课表已公布，是否查看？",
        subtitle = "",
        actionLabel = "查看",
        showDismiss = true
    )
    SemesterPanelMode.BACK -> PanelCopy(
        title = "正在查看下学期课表",
        subtitle = "",
        actionLabel = "返回本学期",
        showDismiss = false
    )
    SemesterPanelMode.NONE -> PanelCopy("", "", "", false)
}

@Composable
private fun SemesterEndFloatingPanel(
    mode: SemesterPanelMode,
    nextTermLabel: String,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (mode == SemesterPanelMode.NONE) return
    val copy = panelCopy(mode, nextTermLabel)
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // 主标题
            Text(
                text = copy.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (copy.subtitle.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                // 副标题
                Text(
                    text = copy.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(14.dp))

            // 按钮行：左「暂时忽略」（可选） + 右主操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (copy.showDismiss) {
                    // 左侧：文字按钮（强调色）
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isLoading
                    ) {
                        Text(
                            "暂时忽略",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }

                // 右侧：胶囊按钮
                Button(
                    onClick = onPrimary,
                    enabled = !isLoading,
                    shape = MaterialTheme.shapes.large,
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(copy.actionLabel, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}