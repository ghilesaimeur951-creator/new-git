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
        versionCode = 12
        versionName = "4.4"
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
                "META-INF/versions/9/OSGI-INF/MANIFEST.MF",
                "OSGI-INF/**"
            )
        }
    }
}

dependencies {
    implementation("org.eclipse.jgit:org.eclipse.jgit:7.7.1.202607240634-r")
    implementation("org.eclipse.jgit:org.eclipse.jgit.ssh.apache:7.7.1.202607240634-r")
    implementation("org.bouncycastle:bcprov-jdk18on:1.84")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    testImplementation("junit:junit:4.13.2")
}


tasks.withType<Test>().configureEach {
    testLogging {
        events("failed", "standardOut", "standardError")
        showStandardStreams = true
    }
}
