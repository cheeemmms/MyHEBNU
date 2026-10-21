package com.myhebnu.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * 旧版午夜刷新 Worker。
 *
 * 新链路见 [WidgetTickWorker]；这个类保留是因为老版本用户设备上可能还残留
 * `schedule_widget_midnight` 周期任务（按类名反序列化），删掉会让 WorkManager
 * 在应用尚未启动时抛 ClassNotFound。它现在只做两件事：刷新一次、把旧任务并入新链。
 */
class ScheduleWidgetWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        updateAllWidgets(applicationContext)
        WidgetTickScheduler.scheduleNext(applicationContext)
        return Result.success()
    }
}
