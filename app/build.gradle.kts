plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22"
}

android {
    namespace = "com.nonton.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nonton.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn"
        )
    }

    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1,LICENSE,NOTICE}"
        }
        jniLibs {
            pickFirsts += "lib/*/libc++_shared.so"
        }
    }

    // Product flavors for different form factors
    flavorDimensions += "device"
    productFlavors {
        create("phone") {
            dimension = "device"
            manifestPlaceholders["flavorManifest"] = "src/phone/AndroidManifest.xml"
        }
        create("tablet") {
            dimension = "device"
            manifestPlaceholders["flavorManifest"] = "src/tablet/AndroidManifest.xml"
        }
        create("tv") {
            dimension = "device"
            manifestPlaceholders["flavorManifest"] = "src/tv/AndroidManifest.xml"
        }
    }
}

configurations.all {
    resolutionStrategy {
        eachDependency {
            if (requested.group == "org.jetbrains.kotlin" && requested.name.startsWith("kotlin-stdlib")) {
                useVersion("1.9.22")
                because("Keep the Kotlin standard library aligned with the compiler")
            }
        }
        // Force Kotlin stdlib version for all configurations
        force("org.jetbrains.kotlin:kotlin-stdlib:1.9.22")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.9.22")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.22")
    }
}

dependencies {
    // Core Android
    val activity_version = "1.9.0"
    val lifecycle_version = "2.7.0"
    val navigation_version = "2.7.6"
    val room_version = "2.6.1"
    val hilt_version = "2.48"
    val work_version = "2.9.0"
    val paging_version = "3.2.1"
    val material_version = "1.11.0"

    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.22")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.9.22")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.22")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:$material_version")
    implementation("androidx.activity:activity-compose:$activity_version")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycle_version")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:$lifecycle_version")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycle_version")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycle_version")

    // Compose BOM
    val compose_bom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(compose_bom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-window-size-class")
    implementation("androidx.compose.runtime:runtime-livedata")
    implementation("androidx.compose.runtime:runtime-rxjava2")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.foundation:foundation-layout")
    // Material 1 provides the pull-refresh API compatible with Material3 1.2.0.
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.animation:animation-graphics")
    implementation("androidx.constraintlayout:constraintlayout-compose:1.1.0")

    // Navigation
    implementation("androidx.navigation:navigation-compose:$navigation_version")
    implementation("androidx.navigation:navigation-fragment-ktx:$navigation_version")

    // Hilt DI
    implementation("com.google.dagger:hilt-android:$hilt_version")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    ksp("com.google.dagger:hilt-compiler:$hilt_version")

    // Room Database
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    ksp("androidx.room:room-compiler:$room_version")

    // Paging
    implementation("androidx.paging:paging-compose:$paging_version")
    implementation("androidx.paging:paging-runtime-ktx:$paging_version")

    // WorkManager (for downloads)
    implementation("androidx.work:work-runtime-ktx:$work_version")

    // Media3 ExoPlayer (Video Playback)
    val media3_version = "1.3.1"
    implementation("androidx.media3:media3-exoplayer:$media3_version")
    implementation("androidx.media3:media3-datasource:$media3_version")
    implementation("androidx.media3:media3-exoplayer-dash:$media3_version")
    implementation("androidx.media3:media3-exoplayer-hls:$media3_version")

    // Coil (Image Loading)
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Serialization (JSON)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Coroutines & Flow
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // OkHttp (untuk scraping provider)
    api("com.squareup.okhttp3:okhttp:4.12.0")
    api("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // JSoup (HTML parser untuk scraper provider)
    implementation("org.jsoup:jsoup:1.18.1")

    // Retrofit & Moshi (API - opsional, untuk kompatibilitas)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")

    // TV Support
    implementation("androidx.fragment:fragment-ktx:1.8.1")
    implementation("androidx.leanback:leanback:1.2.0-alpha03")
    implementation("androidx.leanback:leanback-preference:1.2.0-alpha03")

    // Media3 Session (Media controls, notifications)
    implementation("androidx.media3:media3-session:$media3_version")
    implementation("androidx.media3:media3-ui:$media3_version")

    // Test
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.6.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.6.1")
}