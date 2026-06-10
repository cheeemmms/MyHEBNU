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
        val tag = "MyHEBNU-Widget"
        android.util.Log.d(tag, "MediumWidget.provideGlance: START id=$id")
        try {
            val state = loadDaySchedule(context)
            android.util.Log.d(tag, "MediumWidget.provideGlance: state=$state, calling provideContent")
            provideContent { GlanceTheme { MediumWidgetContent(state, context) } }
            android.util.Log.d(tag, "MediumWidget.provideGlance: DONE")
        } catch (e: Exception) {
            android.util.Log.e(tag, "MediumWidget.provideGlance: EXCEPTION ${e.javaClass.simpleName}: ${e.message}", e)
            throw e
        }
    }
}

@Composable
private fun MediumWidgetContent(state: DayScheduleState, context: Context) {
    val isDark = false
    Box(modifier = GlanceModifier.fillMaxSize().background(widgetSurfaceContainer(isDark)).cornerRadius(R.dimen.widget_dp_28)
        .clickable(actionStartActivity(navigateIntent(context, "schedule")))) {
        when (state) {
            is DayScheduleState.HasCourses -> MediumHasCourses(state, isDark)
            is DayScheduleState.Loading, is DayScheduleState.NoData -> MediumEmpty("暂无课表", "请打开应用同步课表", isDark)
            is DayScheduleState.Weekend -> MediumEmpty("周末愉快", "☀️ 好好休息吧", isDark)
            is DayScheduleState.NoCoursesToday -> MediumEmpty("今日无课", "📚 自由安排", isDark)
            is DayScheduleState.AllDoneToday -> MediumEmpty("今日课程已结束", "📚 自由安排", isDark)
        }
    }
}

@Composable
private fun MediumHasCourses(state: DayScheduleState.HasCourses, isDark: Boolean) {
    Column(modifier = GlanceModifier.fillMaxSize().padding(all = R.dimen.widget_dp_12)) {
        val datePrefix = if (state.isTomorrow) "明天 " else ""
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Text("${datePrefix}${state.dateText} ${state.weekdayLabel}", style = TextStyle(color = widgetOnSurfaceVariant(isDark), fontSize = 11.sp))
            Text("第${state.weekNumber}周", style = TextStyle(color = widgetOnSurfaceVariant(isDark), fontSize = 11.sp))
        }
        Spacer(modifier = GlanceModifier.height(R.dimen.widget_dp_10))
        val d = state.courses.take(2)
        d.forEachIndexed { i, c -> MediumCourseRow(c, isDark); if (i < d.size - 1) Spacer(modifier = GlanceModifier.height(R.dimen.widget_dp_8)) }
        Spacer(modifier = GlanceModifier.height(R.dimen.widget_dp_8))
        val r = state.totalCount - d.size
        if (r > 0) Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
            Text("其他${r}节课程", style = TextStyle(color = widgetOnSurfaceVariant(isDark), fontSize = 10.sp))
            Spacer(modifier = GlanceModifier.width(R.dimen.widget_dp_4))
            state.courses.drop(d.size).take(5).forEach { c ->
                Spacer(modifier = GlanceModifier.size(R.dimen.widget_dp_8).background(courseColorResource(c.colorHue, isDark)))
                Spacer(modifier = GlanceModifier.width(R.dimen.widget_dp_2))
            }
        }
    }
}

@Composable
private fun MediumCourseRow(course: WidgetCourse, isDark: Boolean) {
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        Column(modifier = GlanceModifier.width(R.dimen.widget_dp_42), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(course.startTime ?: "", style = TextStyle(color = widgetOnSurfaceVariant(isDark), fontSize = 11.sp, fontWeight = FontWeight.Medium))
            Text(course.endTime ?: "", style = TextStyle(color = widgetOnSurfaceVariant(isDark), fontSize = 11.sp, fontWeight = FontWeight.Medium))
        }
        Spacer(modifier = GlanceModifier.width(R.dimen.widget_dp_6))
        Spacer(modifier = GlanceModifier.width(R.dimen.widget_dp_4).height(R.dimen.widget_dp_40).background(courseColorResource(course.colorHue, isDark)).cornerRadius(R.dimen.widget_dp_2))
        Spacer(modifier = GlanceModifier.width(R.dimen.widget_dp_8))
        Column(modifier = GlanceModifier.fillMaxWidth()) {
            Text(course.courseName, style = TextStyle(color = widgetOnSurface(isDark), fontSize = 14.sp, fontWeight = FontWeight.Bold), maxLines = 1)
            Spacer(modifier = GlanceModifier.height(R.dimen.widget_dp_2))
            Text(courseDetailText(course), style = TextStyle(color = widgetOnSurfaceVariant(isDark), fontSize = 11.sp), maxLines = 1)
        }
    }
}

@Composable
private fun MediumEmpty(t: String, s: String, isDark: Boolean) {
    Column(modifier = GlanceModifier.fillMaxSize().padding(all = R.dimen.widget_dp_12), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = GlanceModifier.height(R.dimen.widget_dp_12))
        Text(t, style = TextStyle(color = widgetOnSurface(isDark), fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center))
        Spacer(modifier = GlanceModifier.height(R.dimen.widget_dp_4))
        Text(s, style = TextStyle(color = widgetOnSurfaceVariant(isDark), fontSize = 11.sp, textAlign = TextAlign.Center))
    }
}
