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
        versionCode = 7
        versionName = "3.2"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
