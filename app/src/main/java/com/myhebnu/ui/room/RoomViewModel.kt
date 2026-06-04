package com.myhebnu.ui.room

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myhebnu.data.local.preferences.UserPreferences
import com.myhebnu.data.repository.RoomRepository
import com.myhebnu.domain.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RoomUiState(
    // Filter options (loaded from API)
    val campuses: List<Building> = listOf(
        Building("2", "校区2"),
        Building("4", "裕华校区")
    ),
    val buildings: List<Building> = emptyList(),
    val periods: List<PeriodSlot> = emptyList(),
    val isLoadingOptions: Boolean = false,

    // Filter values
    val selectedCampusId: String = "4",
    val selectedBuilding: Building? = null,      // null = all
    val selectedWeek: Int = 1,                     // 1-20
    val selectedDays: Set<Int> = emptySet(),       // 1=Mon...7=Sun
    val selectedPeriods: Set<Int> = emptySet(),    // period numbers
    val venueTypeId: String = "02",                // 02 = 多媒体教室
    val semesterYear: String = "2025",
    val semesterTerm: String = "12",

    // Query state
    val isQuerying: Boolean = false,
    val queryResult: List<EmptyRoom>? = null,       // null = not yet queried
    val queryError: String? = null,
    val totalCount: Int = 0
)

@HiltViewModel
class RoomViewModel @Inject constructor(
    private val repository: RoomRepository,
    private val preferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoomUiState())
    val uiState: StateFlow<RoomUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            val year = preferences.currentSemesterYear.first()
            val term = preferences.currentSemesterTerm.first()
            val week = preferences.currentWeek.first()
            val campusId = preferences.campusId.first()

            _uiState.update {
                it.copy(
                    semesterYear = year,
                    semesterTerm = term,
                    selectedWeek = week,
                    selectedCampusId = campusId
                )
            }

            // Load campus building data
            loadCampusData()
        }
    }

    private fun loadCampusData() {
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.update { it.copy(isLoadingOptions = true) }

            val result = repository.getCampusInfo(
                state.selectedCampusId,
                state.semesterYear,
                state.semesterTerm
            )
            result.fold(
                onSuccess = { info ->
                    _uiState.update {
                        it.copy(
                            isLoadingOptions = false,
                            buildings = info.buildings,
                            periods = info.periods
                        )
                    }
                },
                onFailure = {
                    _uiState.update { it.copy(isLoadingOptions = false) }
                }
            )
        }
    }

    fun setCampus(campusId: String) {
        _uiState.update {
            it.copy(
                selectedCampusId = campusId,
                selectedBuilding = null,  // Reset building when campus changes
                queryResult = null
            )
        }
        loadCampusData()
    }

    fun setBuilding(building: Building?) {
        _uiState.update { it.copy(selectedBuilding = building, queryResult = null) }
    }

    fun setWeek(week: Int) {
        _uiState.update { it.copy(selectedWeek = week, queryResult = null) }
    }

    fun toggleDay(day: Int) {
        _uiState.update {
            val newDays = if (day in it.selectedDays) {
                it.selectedDays - day
            } else {
                it.selectedDays + day
            }
            it.copy(selectedDays = newDays, queryResult = null)
        }
    }

    fun togglePeriod(period: Int) {
        _uiState.update {
            val newPeriods = if (period in it.selectedPeriods) {
                it.selectedPeriods - period
            } else {
                it.selectedPeriods + period
            }
            it.copy(selectedPeriods = newPeriods, queryResult = null)
        }
    }

    fun query() {
        viewModelScope.launch {
            val state = _uiState.value

            // Build filter
            val filter = RoomFilter(
                campusId = state.selectedCampusId,
                year = state.semesterYear,
                term = state.semesterTerm,
                venueTypeId = state.venueTypeId,
                building = state.selectedBuilding?.code,
                weekNumber = state.selectedWeek,
                dayOfWeek = if (state.selectedDays.isNotEmpty()) {
                    state.selectedDays.sorted().joinToString(",")
                } else null,
                periodMask = if (state.selectedPeriods.isNotEmpty()) {
                    BitmaskUtil.periodsToMask(state.selectedPeriods.sorted())
                } else null
            )

            _uiState.update { it.copy(isQuerying = true, queryError = null) }

            val result = repository.queryEmptyRooms(filter)
            result.fold(
                onSuccess = { rooms ->
                    _uiState.update {
                        it.copy(
                            isQuerying = false,
                            queryResult = rooms,
                            totalCount = rooms.size
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isQuerying = false,
                            queryError = e.message ?: "查询失败"
                        )
                    }
                }
            )
        }
    }
}
