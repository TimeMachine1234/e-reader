# PageTurn ProGuard Rules

# ============================================================
# General Android / Kotlin
# ============================================================
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes Exceptions
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# Keep Kotlin metadata so reflection works correctly
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}

# ============================================================
# Room
# ============================================================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keepclassmembers class * extends androidx.room.RoomDatabase {
    abstract *;
}
-dontwarn androidx.room.**

# ============================================================
# Hilt / Dagger
# ============================================================
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ActivityComponentManager
-keep @dagger.hilt.android.HiltAndroidApp class *
-keep @dagger.hilt.InstallIn class *
-keep @dagger.Module class *
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
    @javax.inject.Inject <fields>;
}
-dontwarn dagger.**
-dontwarn javax.inject.**

# ============================================================
# Jetpack Compose
# ============================================================
-keep class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Compose compiler generated classes
-keep class **ComposableSingletons* { *; }

# ============================================================
# Protocol Buffers (Lite runtime)
# ============================================================
-keep class com.google.protobuf.** { *; }
-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}
-dontwarn com.google.protobuf.**

# Keep all Proto-generated classes in the app package
-keep class com.pageturn.reader.proto.** { *; }

# ============================================================
# DataStore
# ============================================================
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# ============================================================
# epublib
# ============================================================
-keep class nl.siegmann.epublib.** { *; }
-keepclassmembers class nl.siegmann.epublib.** { *; }
-dontwarn nl.siegmann.epublib.**
-dontwarn org.slf4j.**
-dontwarn org.xmlpull.**
-dontwarn xmlpull.**

# ============================================================
# junrar / CBR
# ============================================================
-keep class com.github.junrar.** { *; }
-keepclassmembers class com.github.junrar.** { *; }
-dontwarn com.github.junrar.**

# ============================================================
# Coil
# ============================================================
-keep class coil.** { *; }
-dontwarn coil.**

# ============================================================
# Data classes (keep all fields for serialization/deserialization)
# ============================================================
# Preserve all data classes in the app package
-keep class com.pageturn.reader.data.model.** { *; }
-keep class com.pageturn.reader.domain.model.** { *; }

# Keep all Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep all Serializable implementations
-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    !private <fields>;
    !private <methods>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ============================================================
# Enums
# ============================================================
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ============================================================
# Android components
# ============================================================
-keep public class * extends android.app.Application
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# ============================================================
# Coroutines
# ============================================================
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ============================================================
# Remove logging in release builds
# ============================================================
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
