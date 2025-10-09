plugins {
    `kotlin-dsl`
    kotlin("plugin.serialization") version "1.9.0"
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
}

kotlin {
    jvmToolchain(17)
}
