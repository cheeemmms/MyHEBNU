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
import kotlinx.coroutines.CancellationException
import java.time.LocalTime

class ScheduleMicroWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val tag = "MyHEBNU-Widget"
        android.util.Log.d(tag, "MicroWidget.provideGlance: START id=$id")
        try {
            val state = loadDaySchedule(context)
            android.util.Log.d(tag, "MicroWidget.provideGlance: state=$state, calling provideContent")
            provideContent { GlanceTheme { MicroWidgetContent(state, context) } }
            android.util.Log.d(tag, "MicroWidget.provideGlance: DONE")
        } catch (e: CancellationException) {
            // 同一实例被新一次 update 取代时 Glance 会取消当前合成，属正常情况，不记错误。
            android.util.Log.d(tag, "MicroWidget.provideGlance: superseded by newer update")
            throw e
        } catch (e: Exception) {
            android.util.Log.e(tag, "MicroWidget.provideGlance: EXCEPTION ${e.javaClass.simpleName}: ${e.message}", e)
            throw e
        }
    }
}

@Composable
private fun MicroWidgetContent(state: DayScheduleState, context: Context) {
    Box(
        modifier = GlanceModifier.fillMaxSize()
            .background(widgetSurfaceContainerLowest())
            .cornerRadius(R.dimen.widget_dp_20)
            .clickable(actionStartActivity(navigateIntent(context, "schedule"))),
        contentAlignment = Alignment.Center
    ) {
        when (state) {
            is DayScheduleState.Loading -> MicroEmpty("正在同步…", "稍后自动刷新")
            is DayScheduleState.NoData -> MicroEmpty("暂无课表", "请打开应用同步")
            is DayScheduleState.Weekend -> MicroEmpty("周末愉快", "☀️ 好好休息吧")
            is DayScheduleState.Vacation -> MicroEmpty("假期中", "☀️ 好好休息吧")
            is DayScheduleState.NoCoursesToday -> MicroEmpty("今日无课", "📚 自由安排")
            is DayScheduleState.AllDoneToday -> MicroEmpty("今日课程已结束", "📚 自由安排")
            is DayScheduleState.HasCourses -> MicroHasCourses(state)
        }
    }
}

@Composable
private fun MicroHasCourses(state: DayScheduleState.HasCourses) {
    // 硬滑动：已上完的课不再出现，微型组件永远展示"当前/下一节"。
    val remaining = WidgetScheduleLogic.remainingCourses(state.courses, state.nextCourseIndex)
    val course = remaining.firstOrNull() ?: return
    val label = WidgetScheduleLogic.microLabel(
        nextIndex = state.nextCourseIndex,
        inProgress = !state.isTomorrow && WidgetScheduleLogic.isInProgress(course, LocalTime.now()),
        isTomorrow = state.isTomorrow
    )
    // 标签已经承担了"明天"的语义，星期这里只留"周四"，避免出现"明天 明天 周四"。
    val weekdayShown = if (state.isTomorrow) weekdayLabel(state.tomorrowDayOfWeek) else state.weekdayLabel
    Column(modifier = GlanceModifier.fillMaxSize().padding(all = R.dimen.widget_dp_12)) {
        Spacer(modifier = GlanceModifier.defaultWeight())
        Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
            Text(label, style = TextStyle(color = widgetPrimary(), fontSize = 11.sp, fontWeight = FontWeight.Medium))
            Spacer(modifier = GlanceModifier.width(R.dimen.widget_dp_8))
            Text(weekdayShown, style = TextStyle(color = widgetOnSurfaceVariant(), fontSize = 11.sp))
        }
        Spacer(modifier = GlanceModifier.height(R.dimen.widget_dp_6))
        Text(course.courseName, modifier = GlanceModifier.fillMaxWidth(),
            style = TextStyle(color = widgetOnSurface(), fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center), maxLines = 1)
        Spacer(modifier = GlanceModifier.height(R.dimen.widget_dp_8))
        Column(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${course.startPeriod}-${course.endPeriod}节", style = TextStyle(color = widgetOnSurfaceVariant(), fontSize = 13.sp), maxLines = 1)
            Spacer(modifier = GlanceModifier.height(R.dimen.widget_dp_2))
            Text(course.classroom, style = TextStyle(color = widgetOnSurfaceVariant(), fontSize = 13.sp), maxLines = 1)
        }
        Spacer(modifier = GlanceModifier.height(R.dimen.widget_dp_6))
        val others = remaining.drop(1)
        if (others.isNotEmpty()) Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("其他${others.size}节", style = TextStyle(color = widgetOnSurfaceVariant(), fontSize = 10.sp))
            Spacer(modifier = GlanceModifier.width(R.dimen.widget_dp_4))
            others.take(5).forEach { c ->
                Spacer(modifier = GlanceModifier.size(R.dimen.widget_dp_8).background(courseColorResource(c.colorHue)))
                Spacer(modifier = GlanceModifier.width(R.dimen.widget_dp_2))
            }
        }
        Spacer(modifier = GlanceModifier.defaultWeight())
    }
}

@Composable
private fun MicroEmpty(t: String, s: String) {
    Column(modifier = GlanceModifier.fillMaxSize().padding(all = R.dimen.widget_dp_12), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = GlanceModifier.height(R.dimen.widget_dp_16))
        Text(t, style = TextStyle(color = widgetOnSurface(), fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center))
        Spacer(modifier = GlanceModifier.height(R.dimen.widget_dp_4))
        Text(s, style = TextStyle(color = widgetOnSurfaceVariant(), fontSize = 11.sp, textAlign = TextAlign.Center))
    }
}
