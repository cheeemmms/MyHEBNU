package com.myhebnu.data.repository

import com.myhebnu.data.remote.EASystemApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GradeRepository @Inject constructor(
    private val api: EASystemApi
) {
    // Phase 4 will implement:
    // - Fetch grade list for a semester
    // - Fetch detailed grade breakdown for a course (jxb_id)
    // - Calculate GPA (4.0 / 5.0 / weighted percentage)
    // - Aggregate all semester data for trend chart
}
