package com.myhebnu.data.repository

import com.myhebnu.BuildConfig
import com.myhebnu.data.local.preferences.UserPreferences
import com.myhebnu.data.remote.GitHubApi
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
    private val preferences: UserPreferences
) {
    /**
     * Check GitHub Releases for a newer version.
     *
     * @param isManual true = user tapped "检查更新" → always returns result for UI display
     *                 false = auto check on app launch → persists the available version to
     *                         DataStore so the Home screen can show an in-app banner (no
     *                         notification permission required).
     */
    suspend fun checkForUpdate(isManual: Boolean): UpdateCheckResult {
        return try {
            val response = githubApi.getLatestRelease()
            if (!response.isSuccessful) {
                if (isManual) {
                    return UpdateCheckResult(UpdateStatus.ERROR)
                }
                // Auto mode: stay silent, keep any previously shown banner as-is.
                return UpdateCheckResult(UpdateStatus.UP_TO_DATE)
            }

            val release = response.body() ?: return UpdateCheckResult(UpdateStatus.UP_TO_DATE)
            val latestVersion = release.tagName.removePrefix("v").removePrefix("V")
            val currentVersion = BuildConfig.VERSION_NAME

            if (!isNewer(latestVersion, currentVersion)) {
                // Confirmed up to date → clear any stale banner.
                preferences.setAvailableUpdateVersion("")
                return UpdateCheckResult(UpdateStatus.UP_TO_DATE)
            }

            // Newer version available.
            if (isManual) {
                preferences.setAvailableUpdateVersion(latestVersion)
                return UpdateCheckResult(UpdateStatus.UPDATE_AVAILABLE, release.tagName, release.htmlUrl)
            }

            // Auto mode: persist so the Home banner can surface it without a notification.
            preferences.setAvailableUpdateVersion(latestVersion)
            UpdateCheckResult(UpdateStatus.UP_TO_DATE)
        } catch (e: Exception) {
            if (isManual) {
                UpdateCheckResult(UpdateStatus.ERROR)
            } else {
                // Auto mode: stay silent on transient errors, keep any existing banner.
                UpdateCheckResult(UpdateStatus.UP_TO_DATE)
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
}
