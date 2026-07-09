# ARYAA ProGuard / R8 Rules
# ─────────────────────────────────────────────────────────────────────────────
# These rules are applied to the release build only (isMinifyEnabled = true).
# The base Android rules from proguard-android-optimize.txt are applied first.

# ─── Kotlin ──────────────────────────────────────────────────────────────────
-keepclassmembers class **$WhenMappings { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# ─── Kotlinx Serialization ───────────────────────────────────────────────────
# Keep all @Serializable classes and their companions so Retrofit can decode them.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable *;
}
-keep class kotlinx.serialization.** { *; }

# ─── Retrofit + OkHttp ───────────────────────────────────────────────────────
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
# Keep all Retrofit interface methods (suspend funs, annotations)
-keepclasseswithmembers interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.**
-dontwarn okio.**

# ─── Hilt / Dagger ───────────────────────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep @javax.inject.Singleton class * { *; }
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
    @javax.inject.Inject <fields>;
}
-dontwarn dagger.**

# ─── Room ────────────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers @androidx.room.Entity class * { *; }

# ─── Jetpack Compose ─────────────────────────────────────────────────────────
# Compose compiler generates classes at build time; names must survive R8.
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ─── AndroidX / Lifecycle ────────────────────────────────────────────────────
-keep class androidx.lifecycle.** { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>();
}

# ─── Encrypted SharedPreferences (Tink) ──────────────────────────────────────
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# ─── Play Services Location ───────────────────────────────────────────────────
-keep class com.google.android.gms.location.** { *; }
-dontwarn com.google.android.gms.**

# ─── Crash reporting (Sentry, future) ────────────────────────────────────────
# Keep class/method names so stack traces in Sentry remain human-readable.
-keepattributes SourceFile, LineNumberTable
-keep public class * extends java.lang.Exception

# ─── ARYAA application classes ───────────────────────────────────────────────
# Data transfer objects (DTOs) decoded from API JSON must not be renamed.
-keep class com.as307.aryaa.data.remote.dto.** { *; }
# Room entities
-keep class com.as307.aryaa.data.local.entity.** { *; }

# ─── Debugging aid ───────────────────────────────────────────────────────────
# Preserve original file names in crash reports (not package names \u2014 R8 still obfuscates).
-renamesourcefileattribute SourceFile
