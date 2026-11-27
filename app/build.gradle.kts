plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "app.bilimorphe"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.bilimorphe"
        minSdk = 27
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    packaging {
        resources {
            excludes.addAll(arrayOf("META-INF/**", "**.bin"))
            merges += "META-INF/xposed/*"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation(libs.dexkit)
    implementation(libs.libxposed.service)
    compileOnly(libs.xposed)
    compileOnly(libs.libxposed.api)
}
