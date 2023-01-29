plugins {
    kotlin("jvm") version "1.8.0"
}

group = "io.github.rusticflare"
version = "0.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.github.ajalt.clikt:clikt:3.1.0")
    implementation("com.sksamuel.scrimage:scrimage-core:4.0.17")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0")
}
