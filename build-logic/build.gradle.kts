plugins {
    `java-gradle-plugin`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.code.gson:gson:2.13.1")
}

gradlePlugin {
    plugins {
        register("credits") {
            id = "net.noiraude.gtnhcredits.build"
            implementationClass = "CreditsPlugin"
        }
    }
}
