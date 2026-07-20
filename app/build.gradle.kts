plugins {
    id("tempus.android.application")
    id("tempus.android.compose")
    id("tempus.android.room")
}

val appVersionCode = providers.gradleProperty("VERSION_CODE")
    .map(String::toInt)
    .get()
val appVersionName = providers.gradleProperty("VERSION_NAME")
    .map(String::trim)
    .get()

require(appVersionCode in 1..2_100_000_000) {
    "VERSION_CODE must be in the range 1..2100000000, but was $appVersionCode"
}
require(appVersionName.isNotEmpty()) {
    "VERSION_NAME must not be blank"
}

android {
    namespace = "com.cappielloantonio.tempo"

    defaultConfig {
        versionCode = appVersionCode
        versionName = appVersionName
    }

    splits {
        abi {
            isEnable = true
            reset()
            //noinspection ChromeOsAbiSupport
            include("armeabi-v7a", "arm64-v8a")
            isUniversalApk = false
        }
    }

    dependenciesInfo {
        // Disables dependency metadata when building APKs (for IzzyOnDroid/F-Droid).
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles (for Google Play).
        includeInBundle = false
    }

    flavorDimensions += "default"

    productFlavors {
        create("tempus") {
            dimension = "default"
            applicationId = "com.eddyizm.tempus"
        }

        create("degoogled") {
            dimension = "default"
            applicationId = "com.eddyizm.degoogled.tempus"
        }
    }

    buildTypes {
        getByName("release") {
            isShrinkResources = true
            isMinifyEnabled = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }

        getByName("debug") {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }
}

dependencies {
    // FFmpeg Decoder (Jellyfin prebuilt)
    implementation(libs.jellyfin.media3.ffmpeg)

    // Koin
    implementation(libs.koin.android)
    implementation(libs.koin.android.compose)

    // Moko
    implementation(libs.moko.mvvm.core)
    implementation(libs.moko.mvvm.flow)
    implementation(libs.moko.mvvm.livedata)
    implementation(libs.moko.resources)
    implementation(libs.moko.resources.compose)
    testImplementation(libs.moko.resources.test)

    // Compose integrations
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    // AndroidX
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.coordinatorlayout)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.documentfile)

    // Android Material
    implementation(libs.google.material)

    // Coil
    implementation(libs.coil.core)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Media3
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.exoplayer.hls)
    add("tempusImplementation", libs.androidx.media3.cast)

    // Gson (Direct dependency since Retrofit is removed)
    implementation(libs.gson)

    // OkHttp (Used for ClientCertManager and other utilities)
    implementation(libs.okhttp)

    // Ktor
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.gson)
    implementation(libs.ktor.client.logging)
    implementation(libs.slf4j.android)

    // Multiplatform Settings
    implementation(libs.multiplatform.settings)

    implementation(libs.sdp.android)
    implementation(libs.custom.activity.on.crash)
    implementation(libs.facebook.shimmer) {
        artifact {
            type = "aar"
        }
    }

    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
}
