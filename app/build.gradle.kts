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
        versionCode = 11
        versionName = "4.3"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
            )
        }
    }
}

dependencies {
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.10.1.202505221210-r")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
}
