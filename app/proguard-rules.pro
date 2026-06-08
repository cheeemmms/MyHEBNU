# MyHEBNU ProGuard Rules

# Attributes
-keepattributes Signature,Exceptions,RuntimeVisibleAnnotations,AnnotationDefault

# Data models — Gson/Retrofit reflection targets
-keep class com.myhebnu.data.remote.** { <fields>; }
-keepclassmembers interface com.myhebnu.data.remote.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# ──────────────────────────────────────────────
# Glance Widget (1.2+)
# ──────────────────────────────────────────────

# Keep GlanceAppWidget and receiver subclasses (instantiated reflectively)
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }

# Keep widget data models (captured inside Glance composable lambdas)
-keep class com.myhebnu.widget.WidgetCourse { *; }
-keep class com.myhebnu.widget.DayScheduleState { *; }
-keep class com.myhebnu.widget.DayScheduleState$HasCourses { *; }

# Keep Hilt entry point for widgets (EntryPointAccessors reflection)
-keep class com.myhebnu.widget.ScheduleWidgetEntryPoint { *; }

# Keep BroadcastReceiver for grid navigation (manifest-registered)
-keep class com.myhebnu.widget.GridNavReceiver { *; }

# Keep Glance internal runtime classes (critical for provideContent dispatch)
-keep class androidx.glance.appwidget.** { *; }
-keepclassmembers class androidx.glance.appwidget.AppWidgetGlanceHostView { *; }
