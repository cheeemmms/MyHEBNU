package com.myhebnu.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import com.myhebnu.R
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
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

class ScheduleMicroWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val tag = "MyHEBNU-Widget"
        android.util.Log.d(tag, "MicroWidget.provideGlance: START id=$id")
        try {
            val state = loadDaySchedule(context)
            android.util.Log.d(tag, "MicroWidget.provideGlance: state=$state, calling provideContent")
            provideContent { GlanceTheme { MicroWidgetContent(state, context) } }
            android.util.Log.d(tag, "MicroWidget.provideGlance: DONE")
        } catch (e: Exception) {
            android.util.Log.e(tag, "MicroWidget.provideGlance: EXCEPTION ${e.javaClass.simpleName}: ${e.message}", e)
            throw e
        }
    }
}

@Composable
private fun MicroWidgetContent(state: DayScheduleState, context: Context) {
    val isDark = false
    Box(
        modifier = GlanceModifier.fillMaxSize()
            .background(widgetSurfaceContainerLowest(isDark))
            .cornerRadius(R.dimen.widget_dp_28)
            .clickable(actionStartActivity(navigateIntent(context, "schedule"))),
        contentAlignment = Alignment.Center
    ) {
        when (state) {
            is DayScheduleState.Loading, is DayScheduleState.NoData -> MicroEmpty("暂无课表", "请打开应用同步", isDark)
            is DayScheduleState.Weekend -> MicroEmpty("周末愉快", "☀️ 好好休息吧", isDark)
            is DayScheduleState.NoCoursesToday -> MicroEmpty("今日无课", "📚 自由安排", isDark)
            is DayScheduleState.AllDoneToday -> MicroEmpty("今日课程已结束", "📚 自由安排", isDark)
            is DayScheduleState.HasCourses -> MicroHasCourses(state, isDark)
        }
    }
}

@Composable
private fun MicroHasCourses(state: DayScheduleState.HasCourses, isDark: Boolean) {
    val course = state.courses[state.nextCourseIndex]
    val label = when {
        state.isTomorrow -> "明天"
        state.nextCourseIndex == 0 && state.courses.isNotEmpty() -> "正在上课"
        state.nextCourseIndex > 0 -> "下节课"
        else -> ""
    }
    val weekdayShown = if (state.isTomorrow) "明天 ${weekdayLabel(state.tomorrowDayOfWeek)}" else state.weekdayLabel
    Column(modifier = GlanceModifier.fillMaxSize().padding(all = R.dimen.widget_dp_12)) {
        Spacer(modifier = GlanceModifier.defaultWeight())
        Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
            Text(label, style = TextStyle(color = widgetPrimary(isDark), fontSize = 11.sp, fontWeight = FontWeight.Medium))
            Spacer(modifier = GlanceModifier.width(R.dimen.widget_dp_8))
            Text(weekdayShown, style = TextStyle(color = widgetOnSurfaceVariant(isDark), fontSize = 11.sp))
        }
        Spacer(modifier = GlanceModifier.height(R.dimen.widget_dp_6))
        Text(course.courseName, modifier = GlanceModifier.fillMaxWidth(),
            style = TextStyle(color = widgetOnSurface(isDark), fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center), maxLines = 1)
        Spacer(modifier = GlanceModifier.height(R.dimen.widget_dp_8))
        Column(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${course.startPeriod}-${course.endPeriod}节", style = TextStyle(color = widgetOnSurfaceVariant(isDark), fontSize = 13.sp), maxLines = 1)
            Spacer(modifier = GlanceModifier.height(R.dimen.widget_dp_2))
            Text(course.classroom, style = TextStyle(color = widgetOnSurfaceVariant(isDark), fontSize = 13.sp), maxLines = 1)
        }
        Spacer(modifier = GlanceModifier.height(R.dimen.widget_dp_6))
        val n = state.totalCount - 1
        if (n > 0) Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("其他${n}节", style = TextStyle(color = widgetOnSurfaceVariant(isDark), fontSize = 10.sp))
            Spacer(modifier = GlanceModifier.width(R.dimen.widget_dp_4))
            state.courses.filterIndexed { idx, _ -> idx != state.nextCourseIndex && state.nextCourseIndex >= 0 }.take(5).forEach { c ->
                Spacer(modifier = GlanceModifier.size(R.dimen.widget_dp_8).background(courseColorResource(c.colorHue, isDark)))
                Spacer(modifier = GlanceModifier.width(R.dimen.widget_dp_2))
            }
        }
        Spacer(modifier = GlanceModifier.defaultWeight())
    }
}

@Composable
private fun MicroEmpty(t: String, s: String, isDark: Boolean) {
    Column(modifier = GlanceModifier.fillMaxSize().padding(all = R.dimen.widget_dp_12), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = GlanceModifier.height(R.dimen.widget_dp_16))
        Text(t, style = TextStyle(color = widgetOnSurface(isDark), fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center))
        Spacer(modifier = GlanceModifier.height(R.dimen.widget_dp_4))
        Text(s, style = TextStyle(color = widgetOnSurfaceVariant(isDark), fontSize = 11.sp, textAlign = TextAlign.Center))
    }
}
