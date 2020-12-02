# Some methods are only called from tests, so make sure the shrinker keeps them.

-keep class android.support.** { *; }
-keep class com.google.common.** { *; }
-keep class android.databinding.** { *; }

-keepclasseswithmembernames class * {
    native <methods>;
    private native <methods>;
    public native <methods>;
    protected native <methods>;
}

# For Androidx
-keep class androidx.core.app.CoreComponentFactory { *; }

# For Guava:
-dontwarn javax.annotation.**
-dontwarn javax.inject.**
-dontwarn sun.misc.Unsafe

# Proguard rules that are applied to your test apk/code.
-ignorewarnings

-keepattributes *Annotation*

-dontnote junit.framework.**
-dontnote junit.runner.**

-dontwarn android.test.**
-dontwarn android.support.test.**
-dontwarn org.junit.**
-dontwarn org.hamcrest.**
-dontwarn com.squareup.javawriter.JavaWriter
# Uncomment this if you use Mockito
-dontwarn org.mockito.**