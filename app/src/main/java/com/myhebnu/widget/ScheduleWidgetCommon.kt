package com.myhebnu.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.unit.ResourceColorProvider
import com.myhebnu.R
import kotlin.math.absoluteValue

// ──────────────────────────────────────────────
// MD3 Color definitions (resource-based)
// ──────────────────────────────────────────────

fun widgetSurfaceColor(@Suppress("UNUSED_PARAMETER") isDark: Boolean = false): ResourceColorProvider = ResourceColorProvider(R.color.widget_surface)
fun widgetSurfaceContainerLowest(@Suppress("UNUSED_PARAMETER") isDark: Boolean = false): ResourceColorProvider = ResourceColorProvider(R.color.widget_surface_container_lowest)
fun widgetSurfaceContainer(@Suppress("UNUSED_PARAMETER") isDark: Boolean = false): ResourceColorProvider = ResourceColorProvider(R.color.widget_surface_container)
fun widgetSurfaceContainerHigh(@Suppress("UNUSED_PARAMETER") isDark: Boolean = false): ResourceColorProvider = ResourceColorProvider(R.color.widget_surface_container_high)
fun widgetOnSurface(@Suppress("UNUSED_PARAMETER") isDark: Boolean = false): ResourceColorProvider = ResourceColorProvider(R.color.widget_on_surface)
fun widgetOnSurfaceVariant(@Suppress("UNUSED_PARAMETER") isDark: Boolean = false): ResourceColorProvider = ResourceColorProvider(R.color.widget_on_surface_variant)
fun widgetPrimary(@Suppress("UNUSED_PARAMETER") isDark: Boolean = false): ResourceColorProvider = ResourceColorProvider(R.color.widget_primary)
fun widgetPrimaryContainer(@Suppress("UNUSED_PARAMETER") isDark: Boolean = false): ResourceColorProvider = ResourceColorProvider(R.color.widget_primary_container)
fun widgetOnPrimaryContainer(@Suppress("UNUSED_PARAMETER") isDark: Boolean = false): ResourceColorProvider = ResourceColorProvider(R.color.widget_on_primary_container)

// ──────────────────────────────────────────────
// Course color from hue → color resource
// Maps hue to 1 of 6 predefined color resources
// ──────────────────────────────────────────────

private val courseColorResources = listOf(
    R.color.widget_course_red, R.color.widget_course_orange, R.color.widget_course_yellow,
    R.color.widget_course_green, R.color.widget_course_blue, R.color.widget_course_purple
)

fun courseColorResource(hue: Int, @Suppress("UNUSED_PARAMETER") isDark: Boolean = false): ResourceColorProvider {
    val bucket = ((hue % 360).absoluteValue / 60).coerceIn(0, 5)
    return ResourceColorProvider(courseColorResources[bucket])
}

// ──────────────────────────────────────────────
// Navigation intent
// ──────────────────────────────────────────────

fun navigateIntent(context: Context, route: String): Intent {
    return Intent(context, com.myhebnu.MainActivity::class.java).apply {
        action = Intent.ACTION_VIEW
        data = Uri.parse("myhebnu://navigate/$route")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
}

// ──────────────────────────────────────────────
// Widget update helper
// ──────────────────────────────────────────────

suspend fun updateAllWidgets(context: Context) {
    val tag = "MyHEBNU-Widget"
    val manager = GlanceAppWidgetManager(context)
    listOf(
        "Micro" to ScheduleMicroWidget(),
        "Medium" to ScheduleMediumWidget(),
        "LargeGrid" to ScheduleLargeGridWidget(),
        "LargeList" to ScheduleLargeListWidget()
    ).forEach { (name, widget) ->
        try {
            val ids = manager.getGlanceIds(widget.javaClass)
            android.util.Log.d(tag, "updateAllWidgets: $name → ${ids.size} glanceId(s)")
            ids.forEach { id ->
                android.util.Log.d(tag, "updateAllWidgets: calling update for $name id=$id")
                widget.update(context, id)
            }
        } catch (e: Exception) {
            android.util.Log.e(tag, "updateAllWidgets: $name failed: ${e.javaClass.simpleName}: ${e.message}", e)
        }
    }
}

// ──────────────────────────────────────────────
// Shared composable utilities
// ──────────────────────────────────────────────

fun courseDetailText(course: WidgetCourse): String {
    val parts = mutableListOf<String>()
    parts.add("${course.startPeriod}-${course.endPeriod}节")
    if (course.classroom.isNotBlank()) parts.add(course.classroom)
    if (course.teacher.isNotBlank()) parts.add(course.teacher)
    return parts.joinToString(" │ ")
}
