// Standard repos first; Aliyun mirrors are only a fallback for CN networks
// where google() / gradlePluginPortal() are blocked. On CI (GitHub Actions)
// they are skipped entirely to avoid flaky plugin resolution.
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        if (java.lang.System.getenv("CI") != "true") {
            maven("https://maven.aliyun.com/repository/gradle-plugin")
            maven("https://maven.aliyun.com/repository/google")
            maven("https://maven.aliyun.com/repository/public")
        }
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        if (java.lang.System.getenv("CI") != "true") {
            maven("https://maven.aliyun.com/repository/google")
            maven("https://maven.aliyun.com/repository/public")
        }
    }
}

rootProject.name = "wenku8-reader"
include(":app")
