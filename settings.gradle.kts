
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

includeBuild("../ModularUI2") {
    dependencySubstitution {
        substitute(module("com.github.GTNewHorizons:ModularUI2")).using(project(":"))
        substitute(module("com.cleanroommc.modularui:modularui2")).using(project(":"))
    }
}
