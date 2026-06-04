package com.myhebnu.data.repository

import com.myhebnu.data.local.db.dao.ScheduleDao
import com.myhebnu.data.local.preferences.UserPreferences
import com.myhebnu.data.remote.EASystemApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleRepository @Inject constructor(
    private val api: EASystemApi,
    private val dao: ScheduleDao,
    private val preferences: UserPreferences
) {
    // Phase 3 will implement:
    // - Fetch schedule from教务系统 via API
    // - Parse response into CourseEntity list
    // - Cache to Room database
    // - Return Flow<List<CourseEntity>> for UI observation
    // - Handle semester switching
    // - Assign course colors
}
