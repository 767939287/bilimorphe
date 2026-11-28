@file:Suppress("UnstableApiUsage")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        mavenLocal()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    val gprUser = settings.providers.gradleProperty("gpr.user")
    val gprKey = settings.providers.gradleProperty("gpr.key")
    repositories {
        // google() 放最前：androidx / AGP 依赖优先从 Google 源解析，
        // 避免 Maven Central 限流（HTTP 429）导致整个仓库被 Gradle 禁用。
        google()
        mavenCentral()
        mavenLocal()
        maven { url = uri("https://jitpack.io") }
        maven(url = "https://api.xposed.info")
        maven {
            // A repository must be specified for some reason. "registry" is a dummy.
            url = uri("https://maven.pkg.github.com/zjns/registry")
            credentials {
                username = gprUser.orNull ?: System.getenv("GITHUB_ACTOR")
                password = gprKey.orNull ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}


include(
    ":xposed",
    ":integrations:app",
    ":integrations:dummy",
    ":integrations:extend",
    ":integrations:ksp",
    ":integrations",
)
rootProject.name = "bilimorphe"
