import com.diffplug.blowdryer.Blowdryer

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("com.diffplug:blowdryer:1.7.1")
    }
}

plugins {
    `java-library`
    application
    id("com.gradleup.shadow")
}

apply(from = Blowdryer.file("spotless.gradle"))

repositories {
    mavenCentral()
}

val gsonVersion: String by gradle.extra
val junitJupiterVersion: String by gradle.extra
val flatLafVersion: String by gradle.extra

dependencies {
    add("implementation", project(":libCredits"))
    // libCredits declares gson as compileOnly (Minecraft provides it at runtime in the mod);
    // the standalone editor must satisfy that runtime dependency itself, but never imports gson.
    add("runtimeOnly", "com.google.code.gson:gson:$gsonVersion")
    add("testRuntimeOnly", "com.google.code.gson:gson:$gsonVersion")
    add("implementation", "com.formdev:flatlaf:$flatLafVersion")
    add("testImplementation", "org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
    add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
    add("compileOnly", "org.jetbrains:annotations:24.1.0")
    add("testCompileOnly", "org.jetbrains:annotations:24.1.0")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
}

version = rootProject.version

application {
    mainClass.set("net.noiraude.creditseditor.CreditsEditorApp")
    applicationName = "credits-editor"
}

tasks.named<ProcessResources>("processResources") {
    inputs.property("version", rootProject.version)
    filteringCharset = "UTF-8"
    filesMatching("version.properties") {
        expand("version" to rootProject.version)
    }
}

tasks.named<JavaExec>("run") {
    args(rootProject.file("src/main/resources"))
    jvmArgs("-ea")
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

val isLinux: Boolean = System.getProperty("os.name").lowercase().contains("linux")
val desktopIconSizes = listOf(16, 32, 64, 128)
val desktopAppId = "gtnh-credits-editor"

fun desktopFile(): File = file("$installPrefix/share/applications/$desktopAppId.desktop")

fun desktopIconFile(size: Int): File =
    file("$installPrefix/share/icons/hicolor/${size}x${size}/apps/$desktopAppId.png")

fun desktopScalableIconFile(): File =
    file("$installPrefix/share/icons/hicolor/scalable/apps/$desktopAppId.svg")

tasks.register("install") {
    group = "distribution"
    description = $$"Install credits-editor to $PREFIX (default: ~/.local)"
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
                $$"CLASSPATH=$APP_HOME/lib/credits-editor.jar",
                $$"CLASSPATH=$APP_HOME/lib/gtnh-credits-editor.jar"
            )
            .replace(
                "DEFAULT_JVM_OPTS=\"\"",
                $$"DEFAULT_JVM_OPTS=\"-Dapp.name=$APP_BASE_NAME\""
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

        if (isLinux) {
            val desktop = desktopFile()
            desktop.parentFile.mkdirs()
            desktop.writeText("""
                [Desktop Entry]
                Type=Application
                Version=1.0
                Name=GTNH Credits Editor
                GenericName=Credits Editor
                Comment=Edit credits.json resource packs for the GTNH Credits mod
                Exec=gtnh-credits-editor %f
                Icon=$desktopAppId
                Terminal=false
                Categories=Development;
                StartupNotify=true
            """.trimIndent() + "\n")

            desktopIconSizes.forEach { size ->
                val src = file("src/main/resources/icons/icon$size.png")
                val dest = desktopIconFile(size)
                dest.parentFile.mkdirs()
                src.copyTo(dest, overwrite = true)
            }

            val scalableSrc = rootProject.file("assets/GTNH-credits.svg")
            val scalableDest = desktopScalableIconFile()
            scalableDest.parentFile.mkdirs()
            scalableSrc.copyTo(scalableDest, overwrite = true)

            println("Installed: $desktop")
            println("Installed icons under: $installPrefix/share/icons/hicolor/")
        }

        println("Make sure $binDest is on your PATH.")
    }
}

tasks.register("uninstall") {
    group = "distribution"
    description = $$"Remove gtnh-credits-editor installed under $PREFIX (default: ~/.local)"
    doLast {
        val removed = mutableListOf<String>()
        val targets = mutableListOf(
            file("$installPrefix/bin/gtnh-credits-editor"),
            file("$installPrefix/bin/gtnh-credits-editor.bat"),
            file("$installPrefix/lib/gtnh-credits-editor.jar"),
            desktopFile(),
            desktopScalableIconFile()
        )
        desktopIconSizes.mapTo(targets, ::desktopIconFile)
        targets.forEach {
            if (it.exists()) {
                it.delete()
                removed += it.path
            }
        }
        if (removed.isEmpty()) println("Nothing to uninstall under $installPrefix.")
        else removed.forEach { println("Removed $it") }
    }
}
