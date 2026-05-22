# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep Retrofit annotations and types intact during obfuscation
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep Moshi Kotlin models and codegen adapters safe but obfuscate other implementations
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keep @com.squareup.moshi.JsonClass class *

# Keep BuildConfig fields but enable overall code obfuscation
-keepclassmembers class com.example.BuildConfig {
    public static final java.lang.String GEMINI_API_KEY;
}

# Keep all databases, entities, DAOs and models intact in com.example.data package
-keep class com.example.data.** { *; }

# Room-specific runtime rules
-keep class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.**

# Keep ViewModels and their constructors
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

