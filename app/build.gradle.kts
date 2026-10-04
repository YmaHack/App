plugins {
    id("com.android.application")
}

android {
    namespace = "com.ymahack.challenge"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ymahack.challenge"
        minSdk = 24
        targetSdk = 35
        versionCode = 6
        versionName = "6.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
