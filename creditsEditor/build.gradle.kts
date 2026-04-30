import com.diffplug.blowdryer.Blowdryer
import java.net.URI

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

// ---------------------------------------------------------------------------
// windowsInstaller
//
// Builds a Windows .exe installer using NSIS. Stages the fat jar, the
// launcher .bat from src/dist/, and a generated .ico (built in-process from
// the existing PNG icons, no external tool required) into a temp directory,
// then invokes makensis on src/dist/installer.nsi.
//
// NSIS is provisioned project-locally into build/tools/nsis/ by extracting
// pinned .deb files from snapshot.debian.org. No sudo, no system install,
// no user prompts; safe for headless CI. The makensis binary is a Linux
// ELF executable that emits Windows .exe artifacts.
//
// Currently supports Linux x86_64 build hosts only. macOS and Windows
// builders should install NSIS through their native package manager and
// ensure 'makensis' is on PATH; the task then uses that instead.
// ---------------------------------------------------------------------------

val isWindows: Boolean = System.getProperty("os.name").lowercase().contains("windows")
val isMac: Boolean = System.getProperty("os.name").lowercase().contains("mac")

val nsisDebVersion = "3.08-3"
val nsisDebSnapshot = "20240601T000000Z"
val nsisDebs: List<String> = listOf(
    "nsis_${nsisDebVersion}_amd64.deb",
    "nsis-common_${nsisDebVersion}_all.deb"
)
val nsisToolDir: File = layout.buildDirectory.dir("tools/nsis").get().asFile
val nsisMakensis: File = nsisToolDir.resolve("usr/bin/makensis")
val nsisShareDir: File = nsisToolDir.resolve("usr/share/nsis")

fun hasCommand(cmd: String): Boolean {
    val probe = if (isWindows) listOf("where", cmd) else listOf("sh", "-c", "command -v $cmd")
    return try {
        ProcessBuilder(probe)
            .redirectErrorStream(true)
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .start()
            .waitFor() == 0
    } catch (_: Exception) {
        false
    }
}

// Parse a Debian .deb (which is a GNU ar archive) and extract the data.tar.*
// payload alongside; then unpack it with the system tar (which transparently
// handles xz / gz / zst). Pure-JVM ar parsing avoids depending on the system
// `ar` binary; we still rely on `tar` because reimplementing xz decompression
// in build.gradle.kts is not worthwhile.
fun extractDeb(debFile: File, destDir: File) {
    val bytes = debFile.readBytes()
    require(bytes.size > 8 && String(bytes, 0, 8, Charsets.US_ASCII) == "!<arch>\n") {
        "${debFile.name} is not a valid ar archive"
    }
    var pos = 8
    while (pos + 60 <= bytes.size) {
        val name = String(bytes, pos, 16, Charsets.US_ASCII).trimEnd().trimEnd('/')
        val size = String(bytes, pos + 48, 10, Charsets.US_ASCII).trim().toLong()
        pos += 60
        if (name.startsWith("data.tar")) {
            val ext = name.removePrefix("data.tar")
            val tarFile = destDir.resolve("__data.tar$ext")
            tarFile.outputStream().use { it.write(bytes, pos, size.toInt()) }
            val rc = ProcessBuilder("tar", "-xf", tarFile.name)
                .directory(destDir)
                .inheritIO()
                .start()
                .waitFor()
            tarFile.delete()
            if (rc != 0) {
                throw GradleException(
                    "tar -xf failed for data payload of ${debFile.name} (exit $rc). " +
                    "Ensure 'tar' (with xz support) is installed."
                )
            }
            return
        }
        pos += size.toInt()
        if (size % 2L == 1L) pos += 1
    }
    throw GradleException("data.tar.* entry not found in ${debFile.name}")
}

fun provisionLocalNsis() {
    if (nsisMakensis.exists() && nsisMakensis.canExecute() && nsisShareDir.isDirectory) return

    if (isWindows || isMac) {
        if (hasCommand("makensis")) return
        val installHint = if (isMac) "Install NSIS with 'brew install nsis'."
                          else "Install NSIS from https://nsis.sourceforge.io/ and ensure it is on PATH."
        throw GradleException(
            "windowsInstaller provisions a project-local NSIS only on Linux x86_64. " +
            "On this host, makensis was not found on PATH. $installHint"
        )
    }

    val arch = System.getProperty("os.arch")
    if (arch != "amd64" && arch != "x86_64") {
        throw GradleException(
            "Project-local NSIS download supports Linux x86_64 only; detected arch=$arch. " +
            "Install nsis through your distribution's package manager and ensure makensis is on PATH."
        )
    }
    if (!hasCommand("tar")) {
        throw GradleException("'tar' is required to unpack NSIS .deb payloads but was not found on PATH.")
    }

    delete(nsisToolDir)
    nsisToolDir.mkdirs()

    nsisDebs.forEach { debName ->
        val url = "https://snapshot.debian.org/archive/debian/$nsisDebSnapshot/pool/main/n/nsis/$debName"
        val debFile = nsisToolDir.resolve(debName)
        logger.lifecycle("Downloading $url")
        URI(url).toURL().openStream().use { input ->
            debFile.outputStream().use { it.write(input.readAllBytes()) }
        }
        extractDeb(debFile, nsisToolDir)
        debFile.delete()
    }

    if (!nsisMakensis.exists()) {
        throw GradleException("Local NSIS provisioning failed: ${nsisMakensis} was not produced.")
    }
    nsisMakensis.setExecutable(true)
    logger.lifecycle("Provisioned local NSIS at $nsisMakensis")
}

fun pngDimensions(png: File): Pair<Int, Int> {
    val b = png.readBytes()
    require(b.size >= 24 && b[0] == 0x89.toByte() && b[1] == 'P'.code.toByte()) {
        "${png.name} is not a PNG file"
    }
    fun beInt(off: Int): Int =
        ((b[off].toInt() and 0xff) shl 24) or
        ((b[off + 1].toInt() and 0xff) shl 16) or
        ((b[off + 2].toInt() and 0xff) shl 8) or
         (b[off + 3].toInt() and 0xff)
    return beInt(16) to beInt(20)
}

fun writeIcoFromPngs(pngs: List<File>, output: File) {
    val payloads = pngs.map { it.readBytes() }
    val dims = pngs.map(::pngDimensions)

    val n = pngs.size
    val headerSize = 6
    val entrySize = 16
    var offset = headerSize + n * entrySize

    output.outputStream().buffered().use { out ->
        out.write(0); out.write(0)         // reserved
        out.write(1); out.write(0)         // type = 1 (icon)
        out.write(n and 0xff); out.write((n ushr 8) and 0xff)

        for (i in 0 until n) {
            val (w, h) = dims[i]
            val size = payloads[i].size
            out.write(if (w >= 256) 0 else w)
            out.write(if (h >= 256) 0 else h)
            out.write(0)                   // color count
            out.write(0)                   // reserved
            out.write(1); out.write(0)     // planes = 1
            out.write(32); out.write(0)    // bpp = 32
            out.write(size and 0xff)
            out.write((size ushr 8) and 0xff)
            out.write((size ushr 16) and 0xff)
            out.write((size ushr 24) and 0xff)
            out.write(offset and 0xff)
            out.write((offset ushr 8) and 0xff)
            out.write((offset ushr 16) and 0xff)
            out.write((offset ushr 24) and 0xff)
            offset += size
        }
        payloads.forEach(out::write)
    }
}

tasks.register("ensureWindowsInstallerTools") {
    group = "distribution"
    description = "Provision a project-local NSIS toolchain under build/tools/nsis (Linux x86_64); on macOS/Windows, verify makensis is on PATH."
    doLast { provisionLocalNsis() }
}

tasks.register("windowsInstaller") {
    group = "distribution"
    description = "Build a Windows .exe installer (NSIS) bundling the fat jar, launcher, and shortcut icon."
    dependsOn("shadowJar", "ensureWindowsInstallerTools")

    val nsiSrc = file("src/dist/installer.nsi")
    val batSrc = file("src/dist/gtnh-credits-editor.bat")
    val iconSrcDir = file("src/main/resources/icons")
    val iconPngs = desktopIconSizes.map { iconSrcDir.resolve("icon$it.png") }

    val safeVersion = rootProject.version.toString().replace(Regex("[^A-Za-z0-9._+-]"), "_")
    val outName = "gtnh-credits-editor-$safeVersion-setup.exe"
    val stagingDir = layout.buildDirectory.dir("tmp/windowsInstaller").get().asFile
    val outFile = layout.buildDirectory.file("distributions/$outName").get().asFile

    inputs.file(nsiSrc)
    inputs.file(batSrc)
    inputs.files(iconPngs)
    inputs.property("version", rootProject.version.toString())
    outputs.file(outFile)

    doLast {
        delete(stagingDir)
        stagingDir.mkdirs()
        outFile.parentFile.mkdirs()

        val shadow = tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar")
            .get().archiveFile.get().asFile
        shadow.copyTo(stagingDir.resolve("gtnh-credits-editor.jar"), overwrite = true)
        batSrc.copyTo(stagingDir.resolve("gtnh-credits-editor.bat"), overwrite = true)
        nsiSrc.copyTo(stagingDir.resolve("installer.nsi"), overwrite = true)
        writeIcoFromPngs(iconPngs, stagingDir.resolve("gtnh-credits-editor.ico"))

        // Prefer the project-local makensis when present; otherwise fall back to PATH (macOS/Windows).
        val makensisCmd: String =
            if (nsisMakensis.exists()) nsisMakensis.absolutePath else "makensis"
        val pb = ProcessBuilder(
            makensisCmd,
            "-DVERSION=${rootProject.version}",
            "-DOUTFILE=$outName",
            "installer.nsi"
        ).directory(stagingDir).inheritIO()
        // makensis hard-codes its data path at compile time; when running the
        // project-local binary, NSISDIR must point at the matching share dir
        // so it can locate Stubs, Include, Plugins, and Contrib.
        if (nsisMakensis.exists() && nsisShareDir.isDirectory) {
            pb.environment()["NSISDIR"] = nsisShareDir.absolutePath
        }
        val rc = pb.start().waitFor()
        if (rc != 0) throw GradleException("makensis failed with exit code $rc")

        stagingDir.resolve(outName).copyTo(outFile, overwrite = true)
        println("Windows installer: $outFile")
    }
}
