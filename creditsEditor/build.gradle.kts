plugins {
    `java-library`
    application
    id("com.gradleup.shadow")
}

repositories {
    mavenCentral()
}

val gsonVersion: String by gradle.extra

dependencies {
    implementation(project(":libCredits"))
    implementation("com.google.code.gson:gson:$gsonVersion")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

version = rootProject.version

application {
    mainClass.set("net.noiraude.creditseditor.CreditsEditorApp")
    applicationName = "credits-editor"
}

tasks.named<ProcessResources>("processResources") {
    inputs.property("version", rootProject.version)
    filesMatching("version.properties") {
        expand("version" to rootProject.version)
    }
}

tasks.named<JavaExec>("run") {
    args(rootProject.file("src/main/resources"))
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveBaseName.set("credits-editor")
    archiveClassifier.set("")
    archiveVersion.set("")
}

// ---------------------------------------------------------------------------
// install / uninstall
//
// Installs the Gradle-generated launch scripts (bin/credits-editor,
// bin/credits-editor.bat) and the fat jar (lib/credits-editor.jar) under
// PREFIX. The scripts resolve APP_HOME relative to their own location, so
// they work correctly wherever PREFIX points.
//
// PREFIX is read from, in order:
//   -PPREFIX=<path>  Gradle project property
//   $PREFIX           environment variable
//   ~/.local          default
//
// Usage:
//   ./gradlew :creditsEditor:install
//   ./gradlew :creditsEditor:install -PPREFIX=/usr/local
//   PREFIX=/opt/local ./gradlew :creditsEditor:install
//   ./gradlew :creditsEditor:uninstall
// ---------------------------------------------------------------------------

val installPrefix: String =
    (findProperty("PREFIX") as String?)
        ?: System.getenv("PREFIX")
        ?: "${System.getProperty("user.home")}/.local"

tasks.register("install") {
    group = "distribution"
    description = "Install credits-editor to \$PREFIX (default: ~/.local)"
    dependsOn("installShadowDist")
    doLast {
        val distDir = layout.buildDirectory.dir("install/credits-editor-shadow").get().asFile
        val binDest = file("$installPrefix/bin")
        val libDest = file("$installPrefix/lib")
        binDest.mkdirs()
        libDest.mkdirs()

        val script = distDir.resolve("bin/credits-editor")
        val destScript = file("$binDest/gtnh-credits-editor")
        destScript.writeText(script.readText()
            .replace(
                "CLASSPATH=\$APP_HOME/lib/credits-editor.jar",
                "CLASSPATH=\$APP_HOME/lib/gtnh-credits-editor.jar"
            )
            .replace(
                "DEFAULT_JVM_OPTS=\"\"",
                "DEFAULT_JVM_OPTS=\"-Dapp.name=\$APP_BASE_NAME\""
            )
        )
        destScript.setExecutable(true)

        val bat = distDir.resolve("bin/credits-editor.bat")
        file("$binDest/gtnh-credits-editor.bat").writeText(bat.readText()
            .replace("credits-editor.jar", "gtnh-credits-editor.jar")
            .replace("set DEFAULT_JVM_OPTS=\"\"", "set DEFAULT_JVM_OPTS=\"-Dapp.name=%APP_BASE_NAME%\"")
        )

        distDir.resolve("lib/credits-editor.jar").copyTo(file("$libDest/gtnh-credits-editor.jar"), overwrite = true)

        println("Installed: $binDest/gtnh-credits-editor")
        println("Make sure $binDest is on your PATH.")
    }
}

tasks.register("uninstall") {
    group = "distribution"
    description = "Remove gtnh-credits-editor installed under \$PREFIX (default: ~/.local)"
    doLast {
        val removed = mutableListOf<String>()
        listOf(
            file("$installPrefix/bin/gtnh-credits-editor"),
            file("$installPrefix/bin/gtnh-credits-editor.bat"),
            file("$installPrefix/lib/gtnh-credits-editor.jar")
        ).forEach {
            if (it.exists()) {
                it.delete()
                removed += it.path
            }
        }
        if (removed.isEmpty()) println("Nothing to uninstall under $installPrefix.")
        else removed.forEach { println("Removed $it") }
    }
}