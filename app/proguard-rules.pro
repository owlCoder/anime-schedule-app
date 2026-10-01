# Project-specific R8 rules. The default optimize file and consumer rules of each library cover the rest.

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
-keep,includedescriptorclasses class com.owlcoder.animeschedule.**$$serializer { *; }
-keepclassmembers class com.owlcoder.animeschedule.** {
    *** Companion;
}
-keepclasseswithmembers class com.owlcoder.animeschedule.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
