package com.myhebnu.data.repository

import com.myhebnu.data.remote.EASystemApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExamRepository @Inject constructor(
    private val api: EASystemApi
) {
    // Phase 6 will implement:
    // - Fetch exam schedule for a semester
    // - Calculate days remaining until each exam
    // - Sort by exam date
}
