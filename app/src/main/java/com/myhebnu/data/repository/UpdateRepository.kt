package com.myhebnu.data.repository

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.myhebnu.BuildConfig
import com.myhebnu.MyHebnuApplication
import com.myhebnu.R
import com.myhebnu.data.local.preferences.UserPreferences
import com.myhebnu.data.remote.GitHubApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

enum class UpdateStatus { IDLE, CHECKING, UP_TO_DATE, UPDATE_AVAILABLE, ERROR }

data class UpdateCheckResult(
    val status: UpdateStatus,
    val latestVersion: String = "",
    val releaseUrl: String = ""
)

@Singleton
class UpdateRepository @Inject constructor(
    private val githubApi: GitHubApi,
    private val preferences: UserPreferences,
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val NOTIFICATION_ID_UPDATE = 1001
    }

    /**
     * Check GitHub Releases for a newer version.
     *
     * @param isManual true = user tapped "检查更新" → always returns result
     *                 false = auto check on app launch → only notifies if new + not dismissed
     */
    suspend fun checkForUpdate(isManual: Boolean): UpdateCheckResult {
        return try {
            val response = githubApi.getLatestRelease()
            if (!response.isSuccessful) {
                if (isManual) {
                    return UpdateCheckResult(UpdateStatus.ERROR)
                }
                return UpdateCheckResult(UpdateStatus.UP_TO_DATE) // silent for auto
            }

            val release = response.body() ?: return UpdateCheckResult(UpdateStatus.UP_TO_DATE)
            val latestVersion = release.tagName.removePrefix("v").removePrefix("V")
            val currentVersion = BuildConfig.VERSION_NAME

            if (!isNewer(latestVersion, currentVersion)) {
                return UpdateCheckResult(UpdateStatus.UP_TO_DATE)
            }

            // New version available
            val dismissedVersion = preferences.dismissedUpdateVersion.first()

            if (!isManual && latestVersion == dismissedVersion) {
                // User already dismissed this version — don't notify again
                return UpdateCheckResult(UpdateStatus.UP_TO_DATE)
            }

            if (!isManual) {
                // Auto mode: show notification + record dismissal
                showUpdateNotification(release.tagName, release.htmlUrl)
                preferences.setDismissedUpdateVersion(latestVersion)
                return UpdateCheckResult(UpdateStatus.UP_TO_DATE) // UI doesn't need to show
            }

            // Manual mode: return result for UI display
            UpdateCheckResult(UpdateStatus.UPDATE_AVAILABLE, release.tagName, release.htmlUrl)
        } catch (e: Exception) {
            if (isManual) {
                UpdateCheckResult(UpdateStatus.ERROR)
            } else {
                UpdateCheckResult(UpdateStatus.UP_TO_DATE) // silent for auto
            }
        }
    }

    /**
     * Compare two semantic version strings (major.minor.patch).
     * Returns true if [latest] is strictly greater than [current].
     */
    private fun isNewer(latest: String, current: String): Boolean {
        val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }
        if (latestParts.isEmpty() || currentParts.isEmpty()) return false

        val maxLen = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false // equal versions
    }

    /**
     * Show a notification informing the user about a new version.
     * Tapping the notification opens the GitHub release page in the browser.
     */
    private fun showUpdateNotification(tagName: String, htmlUrl: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(htmlUrl)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, MyHebnuApplication.CHANNEL_APP_UPDATE)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notification_update_title, tagName))
            .setContentText(context.getString(R.string.notification_update_body))
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(context.getString(R.string.notification_update_body)))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_UPDATE, notification)
        } catch (e: SecurityException) {
            // Notification permission not granted — silently skip
        }
    }
}
