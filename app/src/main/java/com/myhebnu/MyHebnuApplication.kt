package com.myhebnu

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.myhebnu.widget.ScheduleWidgetWorker
import com.myhebnu.worker.ClassReminderWorker
import com.myhebnu.worker.ExamReminderWorker
import dagger.hilt.android.HiltAndroidApp
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class MyHebnuApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        scheduleWidgetMidnightRefresh()
        scheduleClassReminder()
        scheduleExamReminder()
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)

        // Class reminder channel
        val classChannel = NotificationChannel(
            CHANNEL_CLASS_REMINDER,
            getString(R.string.channel_class_reminder),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.channel_class_reminder_desc)
        }

        // Exam reminder channel
        val examChannel = NotificationChannel(
            CHANNEL_EXAM_REMINDER,
            getString(R.string.channel_exam_reminder),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.channel_exam_reminder_desc)
        }

        // App update channel
        val appUpdateChannel = NotificationChannel(
            CHANNEL_APP_UPDATE,
            getString(R.string.channel_app_update),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = getString(R.string.channel_app_update_desc)
        }

        manager.createNotificationChannels(listOf(classChannel, examChannel, appUpdateChannel))
    }

    /**
     * Schedule a periodic WorkManager task that refreshes all widgets at midnight.
     * This ensures the widget displays the correct date and course list each day.
     */
    private fun scheduleWidgetMidnightRefresh() {
        val now = LocalTime.now()
        val midnight = LocalTime.MIDNIGHT
        var delayMs = ChronoUnit.MILLIS.between(now, midnight)
        if (delayMs <= 0) delayMs += 24 * 60 * 60 * 1000L

        val request = PeriodicWorkRequestBuilder<ScheduleWidgetWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "schedule_widget_midnight",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    /**
     * Schedule a 15-minute periodic check for upcoming classes.
     * Initial delay aligns to the next 15-minute clock boundary.
     */
    private fun scheduleClassReminder() {
        val now = LocalTime.now()
        val minute = now.minute
        val next15 = ((minute / 15) + 1) * 15
        val nextCheck = now.withMinute(0).withSecond(0).withNano(0).plusMinutes(next15.toLong())
        var delayMs = ChronoUnit.MILLIS.between(now, nextCheck)
        if (delayMs <= 0) delayMs += 15 * 60 * 1000L

        val request = PeriodicWorkRequestBuilder<ClassReminderWorker>(15, TimeUnit.MINUTES)
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "class_reminder_periodic",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    /**
     * Schedule a 60-minute periodic check for upcoming exams.
     * First check fires after a 5-minute initial delay.
     */
    private fun scheduleExamReminder() {
        val request = PeriodicWorkRequestBuilder<ExamReminderWorker>(60, TimeUnit.MINUTES)
            .setInitialDelay(5, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "exam_reminder_periodic",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    companion object {
        const val CHANNEL_CLASS_REMINDER = "class_reminder"
        const val CHANNEL_EXAM_REMINDER = "exam_reminder"
        const val CHANNEL_APP_UPDATE = "app_update"
    }
}
