package com.myhebnu.ui.room

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.myhebnu.R
import com.myhebnu.ui.room.components.FilterPanel
import com.myhebnu.ui.room.components.RoomList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomScreen(
    modifier: Modifier = Modifier,
    viewModel: RoomViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_empty_room)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // Filter panel
            item {
                FilterPanel(
                    semesterYear = uiState.semesterYear,
                    onYearChange = viewModel::setYear,
                    semesterTerm = uiState.semesterTerm,
                    onTermChange = viewModel::setTerm,
                    campuses = uiState.campuses,
                    selectedCampusId = uiState.selectedCampusId,
                    onCampusChange = viewModel::setCampus,
                    buildings = uiState.buildings,
                    selectedBuilding = uiState.selectedBuilding,
                    onBuildingChange = viewModel::setBuilding,
                    selectedWeek = uiState.selectedWeek,
                    onWeekChange = viewModel::setWeek,
                    selectedDays = uiState.selectedDays,
                    onDayToggle = viewModel::toggleDay,
                    selectedPeriods = uiState.selectedPeriods,
                    onPeriodToggle = viewModel::togglePeriod,
                    isLoading = uiState.isQuerying,
                    onQuery = viewModel::query
                )
            }

            // Results / states
            item {
                when {
                    uiState.isQuerying -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = stringResource(R.string.loading),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    uiState.queryError != null -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = uiState.queryError ?: "",
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(Modifier.height(8.dp))
                                TextButton(onClick = viewModel::query) {
                                    Text(stringResource(R.string.retry))
                                }
                            }
                        }
                    }
                    uiState.queryResult != null -> {
                        RoomList(
                            rooms = uiState.queryResult!!,
                            totalCount = uiState.totalCount,
                            currentPage = uiState.currentPage,
                            totalPage = uiState.totalPage,
                            isQuerying = uiState.isQuerying,
                            onPageChange = viewModel::goToPage
                        )
                    }
                    else -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "选择筛选条件后点击查询",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
