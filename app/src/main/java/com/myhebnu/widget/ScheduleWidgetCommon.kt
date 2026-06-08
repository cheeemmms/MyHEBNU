package com.myhebnu.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.unit.ColorProvider
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

// ──────────────────────────────────────────────
// MD3 Color definitions (ARGB Int values)
// ──────────────────────────────────────────────

fun widgetSurfaceColor(isDark: Boolean): Int = if (isDark) 0xFF1D1B20.toInt() else 0xFFFFFBFE.toInt()
fun widgetSurfaceContainerLowest(isDark: Boolean): Int = if (isDark) 0xFF141218.toInt() else 0xFFFFF7FF.toInt()
fun widgetSurfaceContainer(isDark: Boolean): Int = if (isDark) 0xFF211F26.toInt() else 0xFFF3EDF7.toInt()
fun widgetSurfaceContainerHigh(isDark: Boolean): Int = if (isDark) 0xFF2B2930.toInt() else 0xFFECE6F0.toInt()
fun widgetOnSurface(isDark: Boolean): Int = if (isDark) 0xFFE6E0E9.toInt() else 0xFF1D1B20.toInt()
fun widgetOnSurfaceVariant(isDark: Boolean): Int = if (isDark) 0xFFCAC4D0.toInt() else 0xFF49454F.toInt()
fun widgetPrimary(isDark: Boolean): Int = if (isDark) 0xFFD0BCFF.toInt() else 0xFF6750A4.toInt()
fun widgetPrimaryContainer(isDark: Boolean): Int = if (isDark) 0xFF4F378B.toInt() else 0xFFEADDFF.toInt()
fun widgetOnPrimaryContainer(isDark: Boolean): Int = if (isDark) 0xFFEADDFF.toInt() else 0xFF21005D.toInt()

// ──────────────────────────────────────────────
// Course color from hue → ARGB Int
// ──────────────────────────────────────────────

fun courseColorFromHueInt(hue: Int, isDark: Boolean): Int {
    val h = (hue % 360).absoluteValue.toFloat()
    val s = if (isDark) 0.50f else 0.55f
    val l = if (isDark) 0.68f else 0.48f
    return hslToColorInt(h, s, l)
}

private fun hslToColorInt(h: Float, s: Float, l: Float): Int {
    val c = (1 - kotlin.math.abs(2 * l - 1)) * s
    val x = c * (1 - kotlin.math.abs((h / 60) % 2 - 1))
    val m = l - c / 2
    val (r1, g1, b1) = when {
        h < 60 -> Triple(c, x, 0f)
        h < 120 -> Triple(x, c, 0f)
        h < 180 -> Triple(0f, c, x)
        h < 240 -> Triple(0f, x, c)
        h < 300 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    val r = ((r1 + m) * 255).roundToInt()
    val g = ((g1 + m) * 255).roundToInt()
    val b = ((b1 + m) * 255).roundToInt()
    return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
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
    val manager = GlanceAppWidgetManager(context)
    listOf(
        ScheduleMicroWidget(),
        ScheduleMediumWidget(),
        ScheduleLargeGridWidget(),
        ScheduleLargeListWidget()
    ).forEach { widget ->
        val ids = manager.getGlanceIds(widget.javaClass)
        ids.forEach { id -> widget.update(context, id) }
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
