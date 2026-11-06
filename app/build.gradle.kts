plugins {
    id("com.android.application")
}

android {
    namespace = "com.mlbb.autoretri"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mlbb.autoretri"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        getByName("debug") { isMinifyEnabled = false }
        getByName("release") { isMinifyEnabled = false }
    }
}

dependencies {}
