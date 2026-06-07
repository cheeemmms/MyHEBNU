package com.myhebnu.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
}
