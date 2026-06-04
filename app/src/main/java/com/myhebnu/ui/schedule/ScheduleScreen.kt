package com.myhebnu.ui.schedule

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.myhebnu.R
import com.myhebnu.ui.schedule.components.WeekSelector
import com.myhebnu.ui.schedule.components.WeekViewGrid
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Week selector bar
            WeekSelector(
                displayWeek = uiState.displayWeek,
                currentWeek = uiState.currentWeek,
                onPreviousWeek = viewModel::goToPreviousWeek,
                onNextWeek = viewModel::goToNextWeek,
                onGoToCurrentWeek = viewModel::goToCurrentWeek
            )

            // Content area
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
                        courses = uiState.courses,
                        dayLabels = uiState.dayLabels,
                        periodLabels = uiState.periodLabels,
                        displayWeek = uiState.displayWeek,
                        currentWeek = uiState.currentWeek,
                        todayDayOfWeek = todayDayOfWeek,
                        activeCourseId = uiState.activeCourseId,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
