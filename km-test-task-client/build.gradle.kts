plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(ktorLibs.plugins.ktor)
    alias(libs.plugins.kotlin.serialization)
}

group = "edu.mmaltsau.interviews"
version = "1.0.0-SNAPSHOT"

application {
    mainClass = "edu.mmaltsau.interviews.MainKt"
}
kotlin {
    jvmToolchain(25)
}
dependencies {
    implementation(ktorLibs.serialization.kotlinx.json)
    implementation(ktorLibs.client.contentNegotiation)
    implementation(ktorLibs.client.logging)
    implementation(ktorLibs.client.core)
    implementation(ktorLibs.client.cio)

    implementation(libs.logback.classic)

    testImplementation(kotlin("test"))
}
