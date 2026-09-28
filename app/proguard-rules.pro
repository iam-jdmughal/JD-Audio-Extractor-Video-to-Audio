# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in getDefaultProguardFile("proguard-android-optimize.txt").

-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}

-dontwarn kotlin.reflect.**
