package com.myhebnu.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myhebnu.BuildConfig
import com.myhebnu.data.local.preferences.UserPreferences
import com.myhebnu.data.repository.UpdateCheckResult
import com.myhebnu.data.repository.UpdateRepository
import com.myhebnu.data.repository.UpdateStatus
import com.myhebnu.ui.theme.ColorPreset
import com.myhebnu.ui.theme.builtInPresets
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: String = "system",
    val currentWeek: Int = 1,
    val semesterYear: String = "2025",
    val semesterTerm: String = "12",
    val advancedEnabled: Boolean = false,
    val appVersion: String = BuildConfig.VERSION_NAME,
    // Update check
    val updateStatus: UpdateStatus = UpdateStatus.IDLE,
    val latestVersion: String = "",
    val releaseUrl: String = "",
    // Custom color
    val useCustomColors: Boolean = false,
    val activePresetId: String? = null,
    val customPresets: List<ColorPreset> = emptyList(),
    val activePreset: ColorPreset? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: UserPreferences,
    private val updateRepository: UpdateRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    /** Exposed for SystemUpdateScreen to read/set auto-check preference. */
    val autoCheckUpdateFlow: Flow<Boolean> = preferences.autoCheckUpdate

    init {
        // Collect each flow independently and merge into UiState
        viewModelScope.launch { preferences.themeMode.collect { v -> _uiState.update { it.copy(themeMode = v) } } }
        viewModelScope.launch { preferences.currentWeek.collect { v -> _uiState.update { it.copy(currentWeek = v) } } }
        viewModelScope.launch { preferences.currentSemesterYear.collect { v -> _uiState.update { it.copy(semesterYear = v) } } }
        viewModelScope.launch { preferences.currentSemesterTerm.collect { v -> _uiState.update { it.copy(semesterTerm = v) } } }
        viewModelScope.launch { preferences.advancedEnabled.collect { v -> _uiState.update { it.copy(advancedEnabled = v) } } }
        viewModelScope.launch { preferences.useCustomColors.collect { v -> _uiState.update { it.copy(useCustomColors = v) } } }
        viewModelScope.launch { preferences.activePresetId.collect { presetId ->
            val json = preferences.customPresetsJson.first()
            val customPresets = parsePresetsJson(json)
            val activePreset = findActivePreset(presetId, customPresets)
            _uiState.update { it.copy(activePresetId = presetId, customPresets = customPresets, activePreset = activePreset) }
        } }
        viewModelScope.launch { preferences.customPresetsJson.collect { json ->
            val customPresets = parsePresetsJson(json)
            val presetId = _uiState.value.activePresetId
            val activePreset = findActivePreset(presetId, customPresets)
            _uiState.update { it.copy(customPresets = customPresets, activePreset = activePreset) }
        } }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }

    fun setCurrentWeek(week: Int) {
        viewModelScope.launch { preferences.setCurrentWeek(week) }
    }

    fun setAdvancedEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setAdvancedEnabled(enabled) }
    }

    // ============================================================
    // Update check
    // ============================================================

    fun setAutoCheckUpdate(enabled: Boolean) {
        viewModelScope.launch { preferences.setAutoCheckUpdate(enabled) }
    }

    fun checkForUpdate() {
        val current = _uiState.value.updateStatus
        if (current == UpdateStatus.CHECKING) return
        // If update already found, tapping opens the release URL instead of re-checking
        if (current == UpdateStatus.UPDATE_AVAILABLE) return

        viewModelScope.launch {
            _uiState.update { it.copy(updateStatus = UpdateStatus.CHECKING) }
            val result = updateRepository.checkForUpdate(isManual = true)
            _uiState.update {
                it.copy(
                    updateStatus = result.status,
                    latestVersion = result.latestVersion,
                    releaseUrl = result.releaseUrl
                )
            }
        }
    }

    // ============================================================
    // Color preset management
    // ============================================================

    fun setUseCustomColors(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setUseCustomColors(enabled)
        }
    }

    fun selectPreset(preset: ColorPreset) {
        viewModelScope.launch {
            preferences.setActivePresetId(preset.id)
            // Auto-enable custom colors when selecting a preset
            if (!_uiState.value.useCustomColors) {
                preferences.setUseCustomColors(true)
            }
        }
    }

    fun createCustomPreset(name: String, seedHue: Float) {
        viewModelScope.launch {
            val preset = ColorPreset(
                id = UUID.randomUUID().toString(),
                name = name,
                seedHue = seedHue,
                isBuiltIn = false
            )
            val updated = _uiState.value.customPresets + preset
            preferences.setCustomPresetsJson(serializePresets(updated))
            // Auto-select newly created preset
            preferences.setActivePresetId(preset.id)
            if (!_uiState.value.useCustomColors) {
                preferences.setUseCustomColors(true)
            }
        }
    }

    fun deleteCustomPreset(preset: ColorPreset) {
        viewModelScope.launch {
            val updated = _uiState.value.customPresets.filter { it.id != preset.id }
            preferences.setCustomPresetsJson(serializePresets(updated))
            // If deleted preset was active, fall back to null
            if (_uiState.value.activePresetId == preset.id) {
                preferences.setActivePresetId(null)
            }
        }
    }

    // ============================================================
    // JSON serialization for custom presets
    // ============================================================

    private fun parsePresetsJson(json: String): List<ColorPreset> {
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                ColorPreset(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    seedHue = obj.getDouble("seedHue").toFloat(),
                    isBuiltIn = false
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    private fun serializePresets(presets: List<ColorPreset>): String {
        val arr = JSONArray()
        for (p in presets) {
            arr.put(JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("seedHue", p.seedHue.toDouble())
            })
        }
        return arr.toString()
    }

    private fun findActivePreset(
        presetId: String?,
        customPresets: List<ColorPreset>
    ): ColorPreset? {
        if (presetId.isNullOrBlank()) return null
        return builtInPresets().find { it.id == presetId }
            ?: customPresets.find { it.id == presetId }
    }
}
