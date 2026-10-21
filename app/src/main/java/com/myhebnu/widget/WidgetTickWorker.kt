package com.myhebnu.widget

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * 小组件刷新的唯一驱动：节次边界一次性任务链。
 *
 * 旧实现是 24h 周期任务，问题有两个：
 * 1. 周期任务在 Doze 下会整体漂移，今天 00:05 跑、明天 00:40 跑；
 * 2. 拖动组件尺寸、切换深色、跨节次这些真正的"内容变化点"都不在它的节拍上。
 *
 * 现在改为：每次执行 → 刷新三个组件 → 算出下一个变化点 → 重新排一次一次性任务。
 * 系统 `updatePeriodMillis=3600000` 保留为兜底节拍（见三个 schedule_widget_*.xml）。
 */
class WidgetTickWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        updateAllWidgets(applicationContext)
        WidgetTickScheduler.scheduleNext(applicationContext)
        return Result.success()
    }
}

/**
 * 负责计算"下一次该在什么时候刷新"并把任务排进 WorkManager。
 *
 * 不申请 `SCHEDULE_EXACT_ALARM`：组件晚几十秒更新并无功能影响，
 * 精确闹钟只对真正的提醒（上课提醒）才值得付出权限成本。
 */
object WidgetTickScheduler {

    private const val WORK_NAME = "widget_tick"

    /** 旧版本遗留的 24h 周期任务名，升级后需要显式取消。 */
    private const val LEGACY_MIDNIGHT_WORK = "schedule_widget_midnight"

    /**
     * 启动（进程启动 / 升级）时调用：清理旧周期任务，重排边界链。
     * 用 REPLACE 而不是 KEEP，保证每次冷启动都校正到最新节次表。
     */
    suspend fun start(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(LEGACY_MIDNIGHT_WORK)
        scheduleNext(context)
    }

    /** 排下一个边界点；由 Worker 自己调用形成链式续期。 */
    suspend fun scheduleNext(context: Context) {
        val boundaries = runCatching { loadTickBoundaries(context) }.getOrDefault(emptyList())
        val delayMinutes = WidgetScheduleLogic.nextDelayMinutes(LocalDateTime.now(), boundaries)
        val request = OneTimeWorkRequestBuilder<WidgetTickWorker>()
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
        android.util.Log.d("MyHEBNU-Widget", "WidgetTickScheduler: next tick in $delayMinutes min")
    }

    /** 取真实节次表；读不到时退化为"只有零点边界"，链仍然能工作。 */
    private suspend fun loadTickBoundaries(context: Context): List<LocalTime> =
        loadWidgetPeriodTimes(context).flatMap { period ->
            listOfNotNull(
                WidgetScheduleLogic.parseTime(period.startTime),
                WidgetScheduleLogic.parseTime(period.endTime)
            )
        }
}
