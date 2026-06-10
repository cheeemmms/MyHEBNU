package com.myhebnu.data.local.preferences

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferences @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private object Keys {
        val CURRENT_SEMESTER_YEAR = stringPreferencesKey("current_semester_year")
        val CURRENT_SEMESTER_TERM = stringPreferencesKey("current_semester_term")
        val CURRENT_WEEK = intPreferencesKey("current_week")
        val SEMESTER_START_DATE = stringPreferencesKey("semester_start_date")
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val SELECTED_LANGUAGE = stringPreferencesKey("selected_language")
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val CAMPUS_ID = stringPreferencesKey("campus_id")
        val STUDENT_NAME = stringPreferencesKey("student_name")
        val ADVANCED_ENABLED = booleanPreferencesKey("advanced_enabled")
        val IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
        val USE_CUSTOM_COLORS = booleanPreferencesKey("use_custom_colors")
        val ACTIVE_PRESET_ID = stringPreferencesKey("active_preset_id")
        val CUSTOM_PRESETS_JSON = stringPreferencesKey("custom_presets_json")
        val DISMISSED_UPDATE_VERSION = stringPreferencesKey("dismissed_update_version")
        val AUTO_CHECK_UPDATE = booleanPreferencesKey("auto_check_update")
        val SENT_REMINDERS = stringPreferencesKey("sent_reminders")
    }

    val currentSemesterYear: Flow<String> = context.dataStore.data.map { it[Keys.CURRENT_SEMESTER_YEAR] ?: "2025" }
    val currentSemesterTerm: Flow<String> = context.dataStore.data.map { it[Keys.CURRENT_SEMESTER_TERM] ?: "12" }
    val currentWeek: Flow<Int> = context.dataStore.data.map { it[Keys.CURRENT_WEEK] ?: 1 }
    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { it[Keys.IS_LOGGED_IN] ?: false }
    val selectedLanguage: Flow<String> = context.dataStore.data.map { it[Keys.SELECTED_LANGUAGE] ?: "zh" }
    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { it[Keys.IS_DARK_MODE] ?: false }
    val themeMode: Flow<String> = context.dataStore.data.map { it[Keys.THEME_MODE] ?: "system" }
    val campusId: Flow<String> = context.dataStore.data.map { it[Keys.CAMPUS_ID] ?: "4" }
    val studentName: Flow<String> = context.dataStore.data.map { it[Keys.STUDENT_NAME] ?: "" }
    val advancedEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.ADVANCED_ENABLED] ?: false }
    val isFirstLaunch: Flow<Boolean> = context.dataStore.data.map { it[Keys.IS_FIRST_LAUNCH] ?: true }
    val useCustomColors: Flow<Boolean> = context.dataStore.data.map { it[Keys.USE_CUSTOM_COLORS] ?: false }
    val activePresetId: Flow<String?> = context.dataStore.data.map { it[Keys.ACTIVE_PRESET_ID] }
    val customPresetsJson: Flow<String> = context.dataStore.data.map { it[Keys.CUSTOM_PRESETS_JSON] ?: "[]" }
    val dismissedUpdateVersion: Flow<String> = context.dataStore.data.map { it[Keys.DISMISSED_UPDATE_VERSION] ?: "" }
    val autoCheckUpdate: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUTO_CHECK_UPDATE] ?: true }

    /** Notification dedup set. Each line is "type|id|yyyy-MM-dd". */
    val sentReminders: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        (prefs[Keys.SENT_REMINDERS] ?: "")
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    }

    suspend fun setCurrentSemester(year: String, term: String) {
        context.dataStore.edit {
            it[Keys.CURRENT_SEMESTER_YEAR] = year
            it[Keys.CURRENT_SEMESTER_TERM] = term
        }
    }

    suspend fun setCurrentWeek(week: Int) {
        context.dataStore.edit { it[Keys.CURRENT_WEEK] = week }
    }

    suspend fun setLoggedIn(value: Boolean) {
        context.dataStore.edit { it[Keys.IS_LOGGED_IN] = value }
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { it[Keys.SELECTED_LANGUAGE] = lang }
    }

    suspend fun setDarkMode(value: Boolean) {
        context.dataStore.edit { it[Keys.IS_DARK_MODE] = value }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode }
    }

    suspend fun setCampusId(id: String) {
        context.dataStore.edit { it[Keys.CAMPUS_ID] = id }
    }

    suspend fun setStudentName(name: String) {
        context.dataStore.edit { it[Keys.STUDENT_NAME] = name }
    }

    suspend fun setAdvancedEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ADVANCED_ENABLED] = enabled }
    }

    suspend fun setFirstLaunchComplete() {
        context.dataStore.edit { it[Keys.IS_FIRST_LAUNCH] = false }
    }

    suspend fun setUseCustomColors(value: Boolean) {
        context.dataStore.edit { it[Keys.USE_CUSTOM_COLORS] = value }
    }

    suspend fun setActivePresetId(id: String?) {
        context.dataStore.edit { it[Keys.ACTIVE_PRESET_ID] = id ?: "" }
    }

    suspend fun setCustomPresetsJson(json: String) {
        context.dataStore.edit { it[Keys.CUSTOM_PRESETS_JSON] = json }
    }

    suspend fun setDismissedUpdateVersion(version: String) {
        context.dataStore.edit { it[Keys.DISMISSED_UPDATE_VERSION] = version }
    }

    suspend fun setAutoCheckUpdate(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_CHECK_UPDATE] = enabled }
    }

    /**
     * Record a sent reminder key to prevent duplicate notifications.
     * Automatically triggers expiry cleanup of old entries.
     */
    suspend fun addSentReminder(key: String) {
        context.dataStore.edit { prefs ->
            val current = (prefs[Keys.SENT_REMINDERS] ?: "")
                .lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toMutableList()
            if (key !in current) {
                current.add(key)
            }
            // Cleanup: remove expired entries and cap at 500
            val today = LocalDate.now()
            val cleaned = cleanExpiredReminders(current, today)
            prefs[Keys.SENT_REMINDERS] = cleaned.joinToString("\n")
        }
    }

    /**
     * Remove entries whose date (last segment of the key) is before [today],
     * then cap at 500 entries (keep the newest).
     */
    private fun cleanExpiredReminders(keys: List<String>, today: LocalDate): List<String> {
        val valid = keys.filter { key ->
            val parts = key.split("|")
            if (parts.size < 3) return@filter true // malformed — keep
            val dateStr = parts.last()
            try {
                val date = LocalDate.parse(dateStr)
                date >= today
            } catch (_: Exception) {
                true // unparseable — keep
            }
        }
        return if (valid.size > 500) valid.takeLast(500) else valid
    }
}
