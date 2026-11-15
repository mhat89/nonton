# ProGuard Rules for Nonton App

# Keep model classes for serialization
-keep class com.nonton.app.data.model.** { *; }
-keepclassmembers class com.nonton.app.data.model.** { *; }

# Keep Room entities
-keep class com.nonton.app.data.local.entity.** { *; }
-keepclassmembers class com.nonton.app.data.local.entity.** { *; }

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.HiltAndroidApp
-keep class * extends dagger.hilt.android.HiltViewModel

# Keep ExoPlayer classes
-keep class com.google.android.exoplayer2.** { *; }
-keepclassmembers class com.google.android.exoplayer2.** { *; }

# Keep Coil classes
-keep class coil3.** { *; }
-keepclassmembers class coil3.** { *; }

# Keep Retrofit/Moshi
-keep class com.squareup.retrofit2.** { *; }
-keepclassmembers class com.squareup.retrofit2.** { *; }
-keep class com.squareup.moshi.** { *; }
-keepclassmembers class com.squareup.moshi.** { *; }

# Keep Kotlinx Serialization
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class kotlinx.serialization.** { *; }

# Keep OkHttp
-keep class okhttp3.** { *; }
-keepclassmembers class okhttp3.** { *; }

# Keep Coroutines
-keep class kotlinx.coroutines.** { *; }
-keepclassmembers class kotlinx.coroutines.** { *; }

# Keep Navigation
-keep class androidx.navigation.** { *; }

# Keep Compose
-keep class androidx.compose.** { *; }

# Keep Room
-keep class androidx.room.** { *; }

# Keep WorkManager
-keep class androidx.work.** { *; }

# Keep Media3
-keep class androidx.media3.** { *; }

# Keep Leanback
-keep class androidx.leanback.** { *; }

# Keep TV Material
-keep class androidx.tv.** { *; }

# Prevent obfuscation of resource names
-keepclassmembers class **.R$* {
    public static <fields>;
}

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep enum values
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep setters in Views so that animations can still work.
-keepclassmembers public class * extends android.view.View {
    void set*(***);
    *** get*();
}

# Keep ParcelableCreator
-keepnames class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Keep Serialization annotations
-keep @kotlinx.serialization.Serializable class * {
    *;
}

# Keep UseCases
-keep class com.nonton.app.domain.usecase.** { *; }

# Keep Repositories
-keep class com.nonton.app.data.repository.** { *; }

# Keep ViewModels
-keep class com.nonton.app.ui.** { *; }

# Keep Application class
-keep class com.nonton.app.NontonApplication { *; }

# ExoPlayer specific
-dontwarn com.google.android.exoplayer2.**

# Coil specific
-dontwarn coil3.**

# OkHttp specific
-dontwarn okhttp3.**

# Retrofit specific
-dontwarn retrofit2.**

# Kotlinx Coroutines specific
-dontwarn kotlinx.coroutines.**

# Kotlinx Serialization specific
-dontwarn kotlinx.serialization.**

# Hilt specific
-dontwarn dagger.hilt.**

# Material3 specific
-dontwarn androidx.compose.material3.**