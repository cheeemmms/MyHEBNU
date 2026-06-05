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
