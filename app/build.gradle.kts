plugins {
    id("com.android.application")
}

android {
    namespace = "com.ghiles.quizubuntu"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ghiles.quizubuntu"
        minSdk = 24
        targetSdk = 35
        versionCode = 6
        versionName = "3.1"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
