package com.myhebnu.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.android.EntryPointAccessors
import java.io.IOException

/**
 * Periodic worker that checks for upcoming classes and sends reminders
 * 15 minutes before each class starts.
 */
class ClassReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext, ReminderEntryPoint::class.java
            )
            entryPoint.reminderManager().checkAndSendClassReminders()
        } catch (e: IOException) {
            Log.w("MyHEBNU", "ClassReminderWorker IO failure, will retry next cycle", e)
        } catch (e: Exception) {
            Log.e("MyHEBNU", "ClassReminderWorker non-retryable failure", e)
        }
        // Always return success to keep the periodic work alive
        return Result.success()
    }
}
