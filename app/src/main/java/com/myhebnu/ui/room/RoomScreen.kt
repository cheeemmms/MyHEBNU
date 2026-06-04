package com.myhebnu.ui.room

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
    viewModel: RoomViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        // Filter panel
        item {
            FilterPanel(
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

        // Query status
        when {
            uiState.isQuerying -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
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
            }
            uiState.queryError != null -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
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
            }
            uiState.queryResult != null -> {
                item {
                    RoomList(
                        rooms = uiState.queryResult!!,
                        totalCount = uiState.totalCount
                    )
                }
            }
            else -> {
                // Initial state — hint text
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
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

        // Bottom spacer
        item { Spacer(Modifier.height(16.dp)) }
    }
}
