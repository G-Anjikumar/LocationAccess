# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Remove debug info
-assumenosideeffects class android.util.Log { *; }

# Obfuscate class, method, and field names
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# Optimize code and remove unused classes
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontpreverify
-dontwarn **

# Keep essential Android components
-keep public class * extends android.app.Application
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.Service
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.view.View
-keepclassmembers class * extends android.app.Application { *; }

# Keep Room database models
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class * extends androidx.room.Entity { *; }

# Keep Hilt-generated classes
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.** { *; }

# Hide API keys from decompilation
-assumenosideeffects class android.content.res.Resources {
    public String getString(int);
}