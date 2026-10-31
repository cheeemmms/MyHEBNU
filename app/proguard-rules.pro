# MyHEBNU ProGuard Rules

# Attributes
-keepattributes Signature,Exceptions,RuntimeVisibleAnnotations,AnnotationDefault

# Data models — Gson/Retrofit reflection targets
-keep class com.myhebnu.data.remote.** { <fields>; }
-keepclassmembers interface com.myhebnu.data.remote.** { *; }

# 「想看」备份文件模型 — 同样由 Gson 反射读写，键名是用户可见的 JSON 契约。
# 不 keep 会出三类事故（v1.7.1 首发就踩了）：
#   1. 字段被改名 → 导出的 JSON 键名变成 a/b/c，跨构建（debug↔release）读不了；
#   2. `items` 的泛型签名退化成裸 ArrayList → Gson 解析出 LinkedTreeMap，
#      导入时抛 `cannot be cast to BackupItem`；
#   3. 只写不读的 `version` 字段被整个裁掉 → 文件里没有版本号。
-keep class com.myhebnu.data.repository.WishlistBackupFile { <fields>; }
-keep class com.myhebnu.data.repository.BackupItem { <fields>; }

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
