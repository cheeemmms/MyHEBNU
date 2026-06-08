package com.myhebnu.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.runBlocking

class GridNavReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_GRID_NAV) return
        val dir = intent.getStringExtra(EXTRA_DIRECTION) ?: return
        runBlocking {
            val p = context.getSharedPreferences("widget_grid_prefs", Context.MODE_PRIVATE)
            var d = p.getInt("day_offset", 0); var per = p.getInt("period_offset", 0)
            when (dir) { "LEFT" -> d = (d - 1).coerceAtLeast(0); "RIGHT" -> d = (d + 1).coerceAtMost(2); "UP" -> per = (per - 1).coerceAtLeast(0); "DOWN" -> per = (per + 1).coerceAtMost(7) }
            p.edit().putInt("day_offset", d).putInt("period_offset", per).apply()
            updateAllWidgets(context)
        }
    }
    companion object { const val ACTION_GRID_NAV = "com.myhebnu.action.GRID_NAV"; const val EXTRA_DIRECTION = "direction" }
}

class ScheduleLargeGridWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = loadDaySchedule(context)
        val prefs = context.getSharedPreferences("widget_grid_prefs", Context.MODE_PRIVATE)
        provideContent { GlanceTheme { LargeGridContent(state, prefs.getInt("day_offset", 0), prefs.getInt("period_offset", 0), context) } }
    }
}

@Composable
private fun LargeGridContent(state: DayScheduleState, dayOff: Int, perOff: Int, context: Context) {
    val isDark = false
    Box(modifier = GlanceModifier.fillMaxSize().background(ColorProvider(widgetSurfaceContainerHigh(isDark))).cornerRadius(28)
        .clickable(actionStartActivity(navigateIntent(context, "schedule")))) {
        when (state) {
            is DayScheduleState.HasCourses -> GridHasCourses(state, dayOff, perOff, isDark, context)
            else -> GridEmpty(state, isDark)
        }
    }
}

@Composable
private fun GridHasCourses(state: DayScheduleState.HasCourses, dayOff: Int, perOff: Int, isDark: Boolean, context: Context) {
    val periods = listOf("08:00" to "08:45", "08:45" to "09:45", "09:45" to "10:30", "10:30" to "11:20", "11:20" to "12:00",
        "14:00" to "14:45", "14:45" to "15:35", "15:35" to "16:35", "16:35" to "17:20", "17:20" to "18:05",
        "19:00" to "19:45", "19:45" to "20:35", "20:35" to "21:20")

    Column(modifier = GlanceModifier.fillMaxSize().padding(all = 10)) {
        // Column headers
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Spacer(modifier = GlanceModifier.width(42))
            for (d in 0..2) {
                val dn = dayOff + d + 1; val today = dn == state.dayOfWeek
                Box(modifier = GlanceModifier.width(56).padding(horizontal = 2), contentAlignment = Alignment.Center) {
                    Text(weekdayShortLabel(dn), style = TextStyle(color = ColorProvider(if (today) widgetPrimary(isDark) else widgetOnSurfaceVariant(isDark)),
                        fontSize = 11.sp, fontWeight = if (today) FontWeight.Bold else FontWeight.Normal), maxLines = 1)
                }
            }
        }
        Spacer(modifier = GlanceModifier.height(4))
        // Grid rows
        for (r in 0..5) {
            val pn = perOff + r + 1
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                Column(modifier = GlanceModifier.width(42), horizontalAlignment = Alignment.CenterHorizontally) {
                    val (s, e) = periods.getOrElse(pn - 1) { "" to "" }
                    Text(s, style = TextStyle(color = ColorProvider(widgetOnSurfaceVariant(isDark)), fontSize = 9.sp))
                    Text(e, style = TextStyle(color = ColorProvider(widgetOnSurfaceVariant(isDark)), fontSize = 9.sp))
                }
                for (d in 0..2) {
                    val dn = dayOff + d + 1
                    val course = state.courses.find { it.dayOfWeek == dn && pn in it.startPeriod..it.endPeriod }
                    val first = course != null && pn == course.startPeriod
                    Box(modifier = GlanceModifier.width(56).height(36).padding(all = 1)
                        .background(ColorProvider(if (course != null) courseColorFromHueInt(course.colorHue, isDark) else widgetSurfaceContainer(isDark)))
                        .cornerRadius(4), contentAlignment = Alignment.Center) {
                        if (first) Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(course!!.courseName, style = TextStyle(color = ColorProvider(widgetOnPrimaryContainer(isDark)), fontSize = 8.sp, fontWeight = FontWeight.Bold), maxLines = 1)
                            Text(course.classroom.take(8), style = TextStyle(color = ColorProvider(widgetOnPrimaryContainer(isDark)), fontSize = 7.sp), maxLines = 1)
                        }
                    }
                }
            }
            if (r < 5) Spacer(modifier = GlanceModifier.height(2))
        }
        Spacer(modifier = GlanceModifier.height(4))
        // Nav buttons
        Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            GridBtn("←", "LEFT", context); Spacer(modifier = GlanceModifier.width(16))
            GridBtn("→", "RIGHT", context); Spacer(modifier = GlanceModifier.width(16))
            GridBtn("↑", "UP", context); Spacer(modifier = GlanceModifier.width(16))
            GridBtn("↓", "DOWN", context)
        }
    }
}

@Composable
private fun GridBtn(label: String, dir: String, context: Context) {
    val intent = Intent(context, GridNavReceiver::class.java).apply { action = GridNavReceiver.ACTION_GRID_NAV; putExtra(GridNavReceiver.EXTRA_DIRECTION, dir) }
    Box(modifier = GlanceModifier.size(28).background(ColorProvider(widgetSurfaceContainer(false))).cornerRadius(14).clickable(actionSendBroadcast(intent)), contentAlignment = Alignment.Center) {
        Text(label, style = TextStyle(color = ColorProvider(widgetOnSurfaceVariant(false)), fontSize = 14.sp))
    }
}

@Composable
private fun GridEmpty(state: DayScheduleState, isDark: Boolean) {
    val t = when (state) { is DayScheduleState.Weekend -> "周末愉快 ☀️"; is DayScheduleState.NoData -> "暂无课表"; is DayScheduleState.NoCoursesToday -> "今日无课"; else -> "" }
    Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(t, style = TextStyle(color = ColorProvider(widgetOnSurfaceVariant(isDark)), fontSize = 14.sp)) }
}
