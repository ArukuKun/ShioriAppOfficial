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

# Proteger modelos de dominio e interfaces para que las extensiones dinámicas no fallen
-keep class com.example.shioriapp.domain.model.** { *; }
-keep class com.example.shioriapp.domain.source.** { *; }
-keep class com.eu.kanade.tachiyomi.** { *; }

# Proteger librerías de red y parsing (Jsoup e Injekt)
-keep class org.jsoup.** { *; }
-keep class uy.kohesive.injekt.** { *; }

# Proteger clases usadas para la serialización de JSON
-keepattributes *Annotation*, InnerClasses
-keep class kotlinx.serialization.** { *; }