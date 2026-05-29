# Add project specific ProGuard rules here.

# errorprone annotations used by tink (androidx.security.crypto dependency)
-dontwarn com.google.errorprone.annotations.**

# OkHttp / Okio
-dontwarn okhttp3.**
-dontwarn okio.**

# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclassmembernames interface * {
    @retrofit2.http.* <methods>;
}

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class rs.owlcoder.animeschedule.**$$serializer { *; }
-keepclassmembers class rs.owlcoder.animeschedule.** {
    *** Companion;
}
-keepclasseswithmembers class rs.owlcoder.animeschedule.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
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