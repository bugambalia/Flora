# Flora: Herbario Musical — R8 / ProGuard Keep Rules

# Preserve line numbers for clean diagnostics
-keepattributes SourceFile,LineNumberTable

# KotlinX Serialization (navigation routes & playlist JSON export/import)
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class com.example.**
-keep,includedescriptorclasses class <1> { *; }
-if @kotlinx.serialization.Serializable class com.example.**
-keep,includedescriptorclasses class <1>$$serializer { *; }

# Room entities, DAOs, and Database
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# DataStore & Preferences
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

# Media3 / ExoPlayer (use targeted Bundleable keep; Media3 ships its own consumer rules)
-dontwarn androidx.media3.**
-keepclassmembers class * implements androidx.media3.common.Bundleable { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.flow.**
