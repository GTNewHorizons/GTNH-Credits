plugins {
    `java-library`
    application
}

repositories {
    mavenCentral()
}

val gsonVersion: String by gradle.extra

dependencies {
    implementation(project(":libCredits"))
    compileOnly("com.google.code.gson:gson:$gsonVersion")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
