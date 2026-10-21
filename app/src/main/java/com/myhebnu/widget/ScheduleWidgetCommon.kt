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
//
// 注意：这里不能接收"是否深色"参数。MIUI 的 RemoteViews 反射执行器把 Glance 生成的
// 每个 Int 参数都当 @ColorRes 解析，颜色一律走资源 ID，深浅色由 values-night 切换。
// ──────────────────────────────────────────────

fun widgetSurfaceColor(): ResourceColorProvider = ResourceColorProvider(R.color.widget_surface)
fun widgetSurfaceContainerLowest(): ResourceColorProvider = ResourceColorProvider(R.color.widget_surface_container_lowest)
fun widgetSurfaceContainer(): ResourceColorProvider = ResourceColorProvider(R.color.widget_surface_container)
fun widgetSurfaceContainerHigh(): ResourceColorProvider = ResourceColorProvider(R.color.widget_surface_container_high)
fun widgetOnSurface(): ResourceColorProvider = ResourceColorProvider(R.color.widget_on_surface)
fun widgetOnSurfaceVariant(): ResourceColorProvider = ResourceColorProvider(R.color.widget_on_surface_variant)
fun widgetPrimary(): ResourceColorProvider = ResourceColorProvider(R.color.widget_primary)
fun widgetPrimaryContainer(): ResourceColorProvider = ResourceColorProvider(R.color.widget_primary_container)
fun widgetOnPrimaryContainer(): ResourceColorProvider = ResourceColorProvider(R.color.widget_on_primary_container)

// ──────────────────────────────────────────────
// Course color from hue → color resource
// Maps hue to 1 of 6 predefined color resources
// ──────────────────────────────────────────────

private val courseColorResources = listOf(
    R.color.widget_course_red, R.color.widget_course_orange, R.color.widget_course_yellow,
    R.color.widget_course_green, R.color.widget_course_blue, R.color.widget_course_purple
)

fun courseColorResource(hue: Int): ResourceColorProvider {
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
