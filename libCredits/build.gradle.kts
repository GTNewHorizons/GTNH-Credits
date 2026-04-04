plugins {
    `java-library`
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("com.google.code.gson:gson:2.2.4")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

// RetroFuturaGradle requires these attributes on all dependencies it resolves.
// libCredits has no Minecraft code, so it is already in 'mcp' (unobfuscated) form.
val rfgObfuscation = Attribute.of("com.gtnewhorizons.retrofuturagradle.obfuscation", String::class.java)
val rfgTransformed = Attribute.of("rfgDeobfuscatorTransformed", Boolean::class.javaObjectType)

configurations.matching { it.name == "runtimeElements" || it.name == "apiElements" }.configureEach {
    attributes {
        attribute(rfgObfuscation, "mcp")
        attribute(rfgTransformed, true)
    }
}
