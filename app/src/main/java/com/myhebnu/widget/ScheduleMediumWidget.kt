package com.myhebnu.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
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
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

class ScheduleMediumWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = loadDaySchedule(context)
        provideContent { GlanceTheme { MediumWidgetContent(state, context) } }
    }
}

@Composable
private fun MediumWidgetContent(state: DayScheduleState, context: Context) {
    val isDark = false
    Box(modifier = GlanceModifier.fillMaxSize().background(ColorProvider(widgetSurfaceContainer(isDark))).cornerRadius(28)
        .clickable(actionStartActivity(navigateIntent(context, "schedule")))) {
        when (state) {
            is DayScheduleState.HasCourses -> MediumHasCourses(state, isDark)
            is DayScheduleState.Loading, is DayScheduleState.NoData -> MediumEmpty("暂无课表", "请打开应用同步课表", isDark)
            is DayScheduleState.Weekend -> MediumEmpty("周末愉快", "☀️ 好好休息吧", isDark)
            is DayScheduleState.NoCoursesToday -> MediumEmpty("今日无课", "📚 自由安排", isDark)
        }
    }
}

@Composable
private fun MediumHasCourses(state: DayScheduleState.HasCourses, isDark: Boolean) {
    Column(modifier = GlanceModifier.fillMaxSize().padding(all = 12)) {
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Text("${state.dateText} ${state.weekdayLabel}", style = TextStyle(color = ColorProvider(widgetOnSurfaceVariant(isDark)), fontSize = 11.sp))
            Text("第${state.weekNumber}周", style = TextStyle(color = ColorProvider(widgetOnSurfaceVariant(isDark)), fontSize = 11.sp))
        }
        Spacer(modifier = GlanceModifier.height(6))
        val d = state.courses.take(2)
        d.forEachIndexed { i, c -> MediumCourseRow(c, isDark); if (i < d.size - 1) Spacer(modifier = GlanceModifier.height(6)) }
        Spacer(modifier = GlanceModifier.height(4))
        val r = state.totalCount - d.size
        if (r > 0) Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
            Text("其他${r}节课程", style = TextStyle(color = ColorProvider(widgetOnSurfaceVariant(isDark)), fontSize = 10.sp))
            Spacer(modifier = GlanceModifier.width(4))
            state.courses.drop(d.size).take(5).forEach { c ->
                Spacer(modifier = GlanceModifier.size(8).background(ColorProvider(courseColorFromHueInt(c.colorHue, isDark))))
                Spacer(modifier = GlanceModifier.width(2))
            }
        }
    }
}

@Composable
private fun MediumCourseRow(course: WidgetCourse, isDark: Boolean) {
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        Column(modifier = GlanceModifier.width(32), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(course.startTime ?: "", style = TextStyle(color = ColorProvider(widgetOnSurfaceVariant(isDark)), fontSize = 11.sp, fontWeight = FontWeight.Medium))
            Text(course.endTime ?: "", style = TextStyle(color = ColorProvider(widgetOnSurfaceVariant(isDark)), fontSize = 11.sp, fontWeight = FontWeight.Medium))
        }
        Spacer(modifier = GlanceModifier.width(6))
        Spacer(modifier = GlanceModifier.width(4).height(40).background(ColorProvider(courseColorFromHueInt(course.colorHue, isDark))).cornerRadius(2))
        Spacer(modifier = GlanceModifier.width(8))
        Column(modifier = GlanceModifier.fillMaxWidth()) {
            Text(course.courseName, style = TextStyle(color = ColorProvider(widgetOnSurface(isDark)), fontSize = 14.sp, fontWeight = FontWeight.Bold), maxLines = 1)
            Spacer(modifier = GlanceModifier.height(2))
            Text(courseDetailText(course), style = TextStyle(color = ColorProvider(widgetOnSurfaceVariant(isDark)), fontSize = 11.sp), maxLines = 1)
        }
    }
}

@Composable
private fun MediumEmpty(t: String, s: String, isDark: Boolean) {
    Column(modifier = GlanceModifier.fillMaxSize().padding(all = 12), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = GlanceModifier.height(12))
        Text(t, style = TextStyle(color = ColorProvider(widgetOnSurface(isDark)), fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center))
        Spacer(modifier = GlanceModifier.height(4))
        Text(s, style = TextStyle(color = ColorProvider(widgetOnSurfaceVariant(isDark)), fontSize = 11.sp, textAlign = TextAlign.Center))
    }
}
