plugins {
    `java-gradle-plugin`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.networknt:json-schema-validator:1.0.87")
}

gradlePlugin {
    plugins {
        create("credits") {
            id = "net.noiraude.gtnhcredits.build"
            implementationClass = "CreditsPlugin"
        }
    }
}