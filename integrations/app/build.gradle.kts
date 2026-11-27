plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

setupLibraryModule {
    namespace = "app.revanced.bilibili.integrations"

    packaging {
        resources {
            excludes += arrayOf("META-INF/**", "kotlin-tooling-metadata.json")
        }
    }

    lint {
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation(projects.integrations.extend)
    implementation(libs.hiddenapibypass)
    implementation(libs.truetypeparser)
    implementation(libs.androidx.documentfile) {
        exclude(libs.androidx.annotation.get().group)
    }
    implementation(libs.kotlinx.serialization.json)
    ksp(projects.integrations.ksp)
    compileOnly(projects.integrations.dummy)
}
