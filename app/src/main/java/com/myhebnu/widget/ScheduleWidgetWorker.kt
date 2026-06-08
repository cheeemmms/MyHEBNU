package com.myhebnu.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Midnight refresh worker — triggers update of all widget instances
 * so they show the correct date and courses for the new day.
 */
class ScheduleWidgetWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        updateAllWidgets(applicationContext)
        return Result.success()
    }
}
