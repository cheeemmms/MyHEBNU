package com.myhebnu.ui.grade

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.myhebnu.R
import com.myhebnu.domain.Grade
import com.myhebnu.ui.grade.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradeScreen(
    modifier: Modifier = Modifier,
    viewModel: GradeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Auto-load grades on every composition entry (tab switch, first launch, etc.)
    LaunchedEffect(Unit) {
        viewModel.loadAllGrades()
    }

    // Show warning in snackbar when refresh fails but cached data is displayed
    LaunchedEffect(uiState.warningMessage) {
        uiState.warningMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearWarning()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
        uiState.isLoading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        uiState.error != null && uiState.semesters.isEmpty() -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = uiState.error ?: stringResource(R.string.error_network),
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = viewModel::loadAllGrades) {
                        Text(stringResource(R.string.retry))
                    }
                }
            }
        }
        uiState.semesters.isEmpty() -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.grade_no_data),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        else -> {
            // Calculate total credits and GPA for current strategy
            val allCourses = uiState.semesters.flatMap { it.courses }
            val totalCredits = allCourses.sumOf { it.credit.toDouble() }.toFloat()
            val currentGpa = when (uiState.currentStrategy) {
                com.myhebnu.domain.GpaStrategy.SCALE_4_0 ->
                    uiState.semesters.firstOrNull()?.gpa4 ?: 0f
                com.myhebnu.domain.GpaStrategy.SCALE_5_0 ->
                    uiState.semesters.firstOrNull()?.gpa5 ?: 0f
                com.myhebnu.domain.GpaStrategy.WEIGHTED_PERCENTAGE ->
                    uiState.semesters.firstOrNull()?.weightedAvg ?: 0f
            }

            LazyColumn(modifier = modifier.fillMaxSize()) {
                // GPA Card
                item {
                    GpaCard(
                        currentStrategy = uiState.currentStrategy,
                        currentGpa = currentGpa,
                        totalCredits = totalCredits,
                        onStrategyChange = viewModel::setStrategy
                    )
                }

                // Trend Chart
                item {
                    GradeTrendChart(
                        semesters = uiState.semesters,
                        currentStrategy = uiState.currentStrategy
                    )
                }

                item { Spacer(Modifier.height(8.dp)) }

                // Semester sections
                items(uiState.semesters, key = { it.semesterName }) { semester ->
                    SemesterSection(
                        semester = semester,
                        isExpanded = uiState.expandedSemester == semester.semesterName,
                        onToggle = { viewModel.toggleSemesterExpanded(semester.semesterName) },
                        onCourseClick = { course -> viewModel.loadGradeDetail(course.classId) }
                    )
                }

                item { Spacer(Modifier.height(80.dp)) } // Bottom padding for FAB
            }
        }
    }

    // Grade detail bottom sheet
    val expandedCourseId = uiState.expandedCourse
    if (expandedCourseId != null) {
        // Find the course in semesters
        val course = uiState.semesters
            .flatMap { it.courses }
            .find { it.classId == expandedCourseId }

        if (course != null) {
            GradeDetailSheet(
                course = course,
                details = uiState.courseDetails[expandedCourseId] ?: emptyList(),
                isLoading = uiState.loadingDetail,
                onDismiss = viewModel::dismissDetail
            )
        }
    }
        } // End Box
    } // End Scaffold
}
