plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "app.bilimorphe"
    compileSdk {
        version = release(37) { minorApiLevel = 2 }
    }

    defaultConfig {
        applicationId = "app.bilimorphe"
        minSdk = 27
        targetSdk = 37
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
    implementation(projects.integrations.app)
    implementation(libs.libxposed.service)
    implementation(libs.dexkit)
    compileOnly(libs.xposed.api)
    compileOnly(libs.libxposed.api)
}
