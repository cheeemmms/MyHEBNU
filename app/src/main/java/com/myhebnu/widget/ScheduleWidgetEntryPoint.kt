package com.myhebnu.widget

import com.myhebnu.data.local.db.AppDatabase
import com.myhebnu.data.local.preferences.UserPreferences
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt entry point for Glance widgets.
 *
 * Widgets run in the RemoteViews process and cannot use standard Hilt injection.
 * This [EntryPoint] provides access to [SingletonComponent] singletons via
 * [dagger.hilt.android.EntryPointAccessors] inside [androidx.glance.GlanceAppWidget.provideGlance].
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ScheduleWidgetEntryPoint {
    fun appDatabase(): AppDatabase
    fun userPreferences(): UserPreferences
}
