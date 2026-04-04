
pluginManagement {
    includeBuild("build-logic")
    repositories {
        maven {
            // RetroFuturaGradle
            name = "GTNH Maven"
            url = uri("https://nexus.gtnewhorizons.com/repository/public/")
            mavenContent {
                includeGroup("com.gtnewhorizons")
                includeGroupByRegex("com\\.gtnewhorizons\\..+")
            }
        }
        gradlePluginPortal()
        mavenCentral()
        mavenLocal()
    }
}

plugins {
    id("com.gtnewhorizons.gtnhsettingsconvention") version("2.0.24")
}

include("libCredits")

val buildProps = java.util.Properties()
settingsDir.resolve("build.properties").inputStream().use(buildProps::load)
buildProps.forEach { key, value -> gradle.extra[key as String] = value as String }
