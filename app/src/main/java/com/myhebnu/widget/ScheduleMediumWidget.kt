package com.myhebnu.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import com.myhebnu.R
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
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
import kotlinx.coroutines.CancellationException

class ScheduleMediumWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val tag = "MyHEBNU-Widget"
        android.util.Log.d(tag, "MediumWidget.provideGlance: START id=$id")
        try {
            val state = loadDaySchedule(context)
            android.util.Log.d(tag, "MediumWidget.provideGlance: state=$state, calling provideContent")
            provideContent { GlanceTheme { MediumWidgetContent(state, context) } }
            android.util.Log.d(tag, "MediumWidget.provideGlance: DONE")
        } catch (e: CancellationException) {
            // 同一实例被新一次 update 取代时 Glance 会取消当前合成，属正常情况，不记错误。
            android.util.Log.d(tag, "MediumWidget.provideGlance: superseded by newer update")
            throw e
        } catch (e: Exception) {
            android.util.Log.e(tag, "MediumWidget.provideGlance: EXCEPTION ${e.javaClass.simpleName}: ${e.message}", e)
            throw e
        }
    }
}

@Composable
private fun MediumWidgetContent(state: DayScheduleState, context: Context) {
    Box(modifier = GlanceModifier.fillMaxSize().background(widgetSurfaceContainer()).cornerRadius(R.dimen.widget_dp_20)
        .clickable(actionStartActivity(navigateIntent(context, "schedule")))) {
        when (state) {
            is DayScheduleState.HasCourses -> MediumHasCourses(state)
            is DayScheduleState.Loading -> MediumEmpty("正在同步…", "稍后自动刷新")
            is DayScheduleState.NoData -> MediumEmpty("暂无课表", "请打开应用同步课表")
            is DayScheduleState.Weekend -> MediumEmpty("周末愉快", "☀️ 好好休息吧")
            is DayScheduleState.Vacation -> MediumEmpty("假期中", "☀️ 好好休息吧")
            is DayScheduleState.NoCoursesToday -> MediumEmpty("今日无课", "📚 自由安排")
            is DayScheduleState.AllDoneToday -> MediumEmpty("今日课程已结束", "📚 自由安排")
        }
    }
}

@Composable
private fun MediumHasCourses(state: DayScheduleState.HasCourses) {
    Column(modifier = GlanceModifier.fillMaxSize().padding(all = R.dimen.widget_dp_12)) {
        val datePrefix = if (state.isTomorrow) "明天 " else ""
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Text("${datePrefix}${state.dateText} ${state.weekdayLabel}", style = TextStyle(color = widgetOnSurfaceVariant(), fontSize = 11.sp))
            Spacer(modifier = GlanceModifier.width(R.dimen.widget_dp_8))
            Text("第${state.weekNumber}周", style = TextStyle(color = widgetOnSurfaceVariant(), fontSize = 11.sp))
        }
        Spacer(modifier = GlanceModifier.height(R.dimen.widget_dp_10))
        // 硬滑动：只展示尚未上完的课，已结束的不再挤回来。
        val remaining = WidgetScheduleLogic.remainingCourses(state.courses, state.nextCourseIndex)
        // 行数随组件高度自适应（拖动放大后能多显示一节），默认 2 行。
        val d = remaining.take(WidgetScheduleLogic.mediumRowsFor(LocalSize.current.height.value))
        d.forEachIndexed { i, c -> MediumCourseRow(c); if (i < d.size - 1) Spacer(modifier = GlanceModifier.height(R.dimen.widget_dp_8)) }
        Spacer(modifier = GlanceModifier.height(R.dimen.widget_dp_8))
        val r = remaining.size - d.size
        if (r > 0) Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
            Text("其他${r}节课程", style = TextStyle(color = widgetOnSurfaceVariant(), fontSize = 10.sp))
            Spacer(modifier = GlanceModifier.width(R.dimen.widget_dp_4))
            remaining.drop(d.size).take(5).forEach { c ->
                Spacer(modifier = GlanceModifier.size(R.dimen.widget_dp_8).background(courseColorResource(c.colorHue)))
                Spacer(modifier = GlanceModifier.width(R.dimen.widget_dp_2))
            }
        }
    }
}

@Composable
private fun MediumCourseRow(course: WidgetCourse) {
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        Column(modifier = GlanceModifier.width(R.dimen.widget_dp_42), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(course.startTime ?: "", style = TextStyle(color = widgetOnSurfaceVariant(), fontSize = 11.sp, fontWeight = FontWeight.Medium))
            Text(course.endTime ?: "", style = TextStyle(color = widgetOnSurfaceVariant(), fontSize = 11.sp, fontWeight = FontWeight.Medium))
        }
        Spacer(modifier = GlanceModifier.width(R.dimen.widget_dp_6))
        Spacer(modifier = GlanceModifier.width(R.dimen.widget_dp_4).height(R.dimen.widget_dp_40).background(courseColorResource(course.colorHue)).cornerRadius(R.dimen.widget_dp_2))
        Spacer(modifier = GlanceModifier.width(R.dimen.widget_dp_8))
        Column(modifier = GlanceModifier.fillMaxWidth()) {
            Text(course.courseName, style = TextStyle(color = widgetOnSurface(), fontSize = 14.sp, fontWeight = FontWeight.Bold), maxLines = 1)
            Spacer(modifier = GlanceModifier.height(R.dimen.widget_dp_2))
            Text(courseDetailText(course), style = TextStyle(color = widgetOnSurfaceVariant(), fontSize = 11.sp), maxLines = 1)
        }
    }
}

@Composable
private fun MediumEmpty(t: String, s: String) {
    Column(modifier = GlanceModifier.fillMaxSize().padding(all = R.dimen.widget_dp_12), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = GlanceModifier.height(R.dimen.widget_dp_12))
        Text(t, style = TextStyle(color = widgetOnSurface(), fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center))
        Spacer(modifier = GlanceModifier.height(R.dimen.widget_dp_4))
        Text(s, style = TextStyle(color = widgetOnSurfaceVariant(), fontSize = 11.sp, textAlign = TextAlign.Center))
    }
}
