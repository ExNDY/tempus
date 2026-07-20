plugins {
    id("com.android.application")
    id("tempus.android.base")
}

android {
    defaultConfig {
        targetSdk = 37
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}
