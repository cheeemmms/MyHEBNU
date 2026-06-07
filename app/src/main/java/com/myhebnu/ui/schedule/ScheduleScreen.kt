package com.myhebnu.ui.schedule

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
    val today = LocalDate.now()
    val todayDayOfWeek = today.dayOfWeek.value // Mon=1 ... Sun=7
    val snackbarHostState = remember { SnackbarHostState() }

    // Show error in snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
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
