package com.myhebnu.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myhebnu.data.local.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isDarkMode: Boolean = false,
    val isDynamicColor: Boolean = true,
    val currentWeek: Int = 1,
    val semesterYear: String = "2025",
    val semesterTerm: String = "12",
    val advancedEnabled: Boolean = false,
    val appVersion: String = ""
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                preferences.isDarkMode,
                preferences.currentWeek,
                preferences.currentSemesterYear,
                preferences.currentSemesterTerm,
                preferences.advancedEnabled
            ) { dark, week, year, term, advanced ->
                SettingsUiState(
                    isDarkMode = dark,
                    currentWeek = week,
                    semesterYear = year,
                    semesterTerm = term,
                    advancedEnabled = advanced,
                    appVersion = "1.0.0"
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch { preferences.setDarkMode(enabled) }
    }

    fun setCurrentWeek(week: Int) {
        viewModelScope.launch { preferences.setCurrentWeek(week) }
    }

    fun setAdvancedEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setAdvancedEnabled(enabled) }
    }
}
