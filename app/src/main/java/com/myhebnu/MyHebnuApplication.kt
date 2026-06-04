package com.myhebnu

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyHebnuApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
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

        manager.createNotificationChannels(listOf(classChannel, examChannel))
    }

    companion object {
        const val CHANNEL_CLASS_REMINDER = "class_reminder"
        const val CHANNEL_EXAM_REMINDER = "exam_reminder"
    }
}
