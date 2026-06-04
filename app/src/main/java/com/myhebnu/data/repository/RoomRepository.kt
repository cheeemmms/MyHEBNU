package com.myhebnu.data.repository

import com.myhebnu.data.remote.EASystemApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomRepository @Inject constructor(
    private val api: EASystemApi
) {
    // Phase 5 will implement:
    // - Fetch campus building list with period info
    // - Fetch venue type options
    // - Query empty rooms with filter parameters
    // - Convert week/period bitmasks
}
