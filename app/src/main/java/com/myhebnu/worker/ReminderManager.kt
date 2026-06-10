package com.myhebnu.worker

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.myhebnu.MyHebnuApplication
import com.myhebnu.MainActivity
import com.myhebnu.R
import com.myhebnu.data.local.db.entity.CourseEntity
import com.myhebnu.data.repository.ExamRepository
import com.myhebnu.data.repository.ScheduleRepository
import com.myhebnu.data.local.preferences.UserPreferences
import com.myhebnu.domain.Exam
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderManager @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val examRepository: ExamRepository,
    private val preferences: UserPreferences,
    @param:ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "MyHEBNU"
        private const val CLASS_NOTIFICATION_BASE = 2000
        private const val EXAM_NOTIFICATION_BASE = 3000
    }

    // ── Class Reminders ────────────────────────────────────────────

    suspend fun checkAndSendClassReminders() {
        val today = LocalDate.now()
        val dayOfWeek = today.dayOfWeek.value // 1=Mon..7=Sun
        val now = LocalTime.now()

        val year = preferences.currentSemesterYear.first()
        val term = preferences.currentSemesterTerm.first()
        val currentWeek = preferences.currentWeek.first()

        // Periods: in-memory cache (no API, no network)
        val periods = scheduleRepository.fetchPeriods(year, term)
        if (periods.isEmpty()) {
            Log.w(TAG, "[ClassReminder] No period data available")
            return
        }

        // Courses for today: Room query (no API, no network)
        val courses = scheduleRepository.getCoursesByDay(year, term, dayOfWeek)
        if (courses.isEmpty()) {
            Log.d(TAG, "[ClassReminder] No courses found for day=$dayOfWeek")
            return
        }

        // Filter by week and odd/even
        val isOdd = currentWeek % 2 == 1
        val validCourses = courses.filter { course ->
            currentWeek in course.startWeek..course.endWeek &&
            when (course.oddEven) {
                1 -> isOdd
                2 -> !isOdd
                else -> true
            }
        }

        val sentKeys = preferences.sentReminders.first()

        for (course in validCourses) {
            val periodStart = periods.find { it.period == course.startPeriod }?.startTime ?: continue
            val periodEnd = periods.find { it.period == course.endPeriod }?.endTime ?: continue

            val start = try {
                LocalTime.parse(periodStart)
            } catch (_: Exception) { continue }

            val windowStart = start.minusMinutes(15)
            if (now < windowStart || now >= start) continue // not in the 15-min window

            val key = "class|${course.id}|$today"
            if (key in sentKeys) continue // already sent

            showClassNotification(course, periodStart, periodEnd)
            preferences.addSentReminder(key)
            Log.d(TAG, "[ClassReminder] Sent: ${course.courseName} at $periodStart")
        }
    }

    private fun showClassNotification(course: CourseEntity, startTime: String, endTime: String) {
        val today = LocalDate.now()
        val notifId = (CLASS_NOTIFICATION_BASE + kotlin.math.abs("class_${course.id}_$today".hashCode()) % 1000)

        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("myhebnu://navigate/schedule")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bigText = context.getString(
            R.string.notification_class_body_big,
            course.courseName, course.classroom, course.teacher, startTime, endTime
        )

        val notification = NotificationCompat.Builder(context, MyHebnuApplication.CHANNEL_CLASS_REMINDER)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notification_class_title))
            .setContentText(
                context.getString(
                    R.string.notification_class_body,
                    course.courseName, course.classroom, course.teacher
                )
            )
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notifId, notification)
        } catch (e: SecurityException) {
            // Notification permission not granted
        }
    }

    // ── Exam Reminders ─────────────────────────────────────────────

    suspend fun checkAndSendExamReminders() {
        val year = preferences.currentSemesterYear.first()
        val term = preferences.currentSemesterTerm.first()

        // Read from Room cache (no API call — data sync is ExamRepository's job)
        val exams = examRepository.getCachedExams(year, term)
        if (exams.isEmpty()) {
            Log.d(TAG, "[ExamReminder] No cached exam data — skipping")
            return
        }

        val today = LocalDate.now()
        val now = LocalTime.now()
        val sentKeys = preferences.sentReminders.first()

        for (exam in exams) {
            // 1 day before notification
            if (exam.examDate == today.plusDays(1)) {
                val key = "exam_day|${exam.id}|${exam.examDate}"
                if (key !in sentKeys) {
                    showExamDayBeforeNotification(exam)
                    preferences.addSentReminder(key)
                    Log.d(TAG, "[ExamReminder] Day-before: ${exam.courseName} on ${exam.examDate}")
                }
            }

            // 1 hour before notification (same day, has start time)
            if (exam.examDate == today && exam.startTime != null) {
                val start = exam.startTime
                if (now >= start.minusHours(1) && now < start) {
                    val key = "exam_hour|${exam.id}|${exam.examDate}"
                    if (key !in sentKeys) {
                        showExamHourBeforeNotification(exam)
                        preferences.addSentReminder(key)
                        Log.d(TAG, "[ExamReminder] Hour-before: ${exam.courseName} at ${exam.startTime}")
                    }
                }
            }
        }
    }

    private fun showExamDayBeforeNotification(exam: Exam) {
        val notifId = EXAM_NOTIFICATION_BASE + kotlin.math.abs("exam_day_${exam.id}".hashCode()) % 1000

        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("myhebnu://navigate/exam")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeStr = Exam.formatTimeRange(exam.startTime, exam.endTime)
        val bigText = context.getString(
            R.string.notification_exam_day_body_big,
            exam.courseName, timeStr, exam.location, exam.seatNumber
        )

        val notification = NotificationCompat.Builder(context, MyHebnuApplication.CHANNEL_EXAM_REMINDER)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notification_exam_day_title))
            .setContentText(
                context.getString(
                    R.string.notification_exam_day_body,
                    exam.courseName, exam.location, timeStr
                )
            )
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notifId, notification)
        } catch (e: SecurityException) {
            // Notification permission not granted
        }
    }

    private fun showExamHourBeforeNotification(exam: Exam) {
        val notifId = EXAM_NOTIFICATION_BASE + kotlin.math.abs("exam_hour_${exam.id}".hashCode()) % 1000

        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("myhebnu://navigate/exam")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeStr = Exam.formatTimeRange(exam.startTime, exam.endTime)
        val bigText = context.getString(
            R.string.notification_exam_hour_body_big,
            exam.courseName, timeStr, exam.location, exam.seatNumber
        )

        val notification = NotificationCompat.Builder(context, MyHebnuApplication.CHANNEL_EXAM_REMINDER)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notification_exam_hour_title))
            .setContentText(
                context.getString(
                    R.string.notification_exam_hour_body,
                    exam.courseName, exam.location, timeStr
                )
            )
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notifId, notification)
        } catch (e: SecurityException) {
            // Notification permission not granted
        }
    }
}
