# MyHEBNU ProGuard Rules

# Retrofit
-keepattributes Signature
-keepattributes Exceptions
-keep class com.myhebnu.data.remote.** { *; }
-keepclassmembers interface com.myhebnu.data.remote.** { *; }

# Gson
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
