pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "creditsEditor"

val buildProps = java.util.Properties()
settingsDir.parentFile.resolve("build.properties").inputStream().use(buildProps::load)
buildProps.forEach { key, value -> gradle.extra[key as String] = value as String }
