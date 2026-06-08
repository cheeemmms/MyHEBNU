package com.myhebnu.widget

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton injected into ViewModels to trigger widget refresh from within the app.
 * After a successful course sync, all widget instances are updated.
 */
@Singleton
class WidgetUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun updateAll() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                updateAllWidgets(context)
            } catch (e: Exception) {
                android.util.Log.e("MyHEBNU", "Widget update failed: ${e.message}", e)
            }
        }
    }
}
