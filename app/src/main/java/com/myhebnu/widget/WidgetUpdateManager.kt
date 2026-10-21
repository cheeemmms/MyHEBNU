package com.myhebnu.widget

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton injected into ViewModels to trigger widget refresh from within the app.
 * After a successful course sync, all widget instances are updated.
 *
 * 旧实现每次调用都新建 `CoroutineScope(Dispatchers.IO)` 且不取消：课表连续同步几次就会
 * 并行重建多次 RemoteViews。现在改为持有应用级 scope（@Singleton 实例 → 进程级生命周期），
 * 并用 Mutex 做"同时只允许一次重建"的并发闸。
 */
@Singleton
class WidgetUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val updateMutex = Mutex()

    fun updateAll() {
        scope.launch {
            if (!updateMutex.tryLock()) {
                android.util.Log.d("MyHEBNU-Widget", "updateAll: skipped, previous update still running")
                return@launch
            }
            try {
                updateAllWidgets(context)
            } catch (e: Exception) {
                android.util.Log.e("MyHEBNU", "Widget update failed: ${e.message}", e)
            } finally {
                updateMutex.unlock()
            }
        }
    }
}
