package com.myhebnu.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.android.EntryPointAccessors
import java.io.IOException

/**
 * Periodic worker that checks for upcoming exams and sends reminders
 * 1 day before and 1 hour before each exam.
 */
class ExamReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext, ReminderEntryPoint::class.java
            )
            entryPoint.reminderManager().checkAndSendExamReminders()
        } catch (e: IOException) {
            Log.w("MyHEBNU", "ExamReminderWorker IO failure, will retry next cycle", e)
        } catch (e: Exception) {
            Log.e("MyHEBNU", "ExamReminderWorker non-retryable failure", e)
        }
        // Always return success to keep the periodic work alive
        return Result.success()
    }
}
