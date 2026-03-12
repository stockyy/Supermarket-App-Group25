val kotlinVersion = "2.3.0"
val logbackVersion = "1.5.20"

val sqliteVersion = "3.51.2.0"
val exposedVersion = "1.1.1"
val h2Version = "2.4.240"

plugins {
    kotlin("jvm") version "2.3.0"
    id("io.ktor.plugin") version "3.4.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.0"
}

group = "com.supermarket"
version = "0.0.1"


application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("io.ktor:ktor-server-host-common")
    implementation("io.ktor:ktor-server-netty")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("io.ktor:ktor-server-config-yaml")
    testImplementation("io.ktor:ktor-server-test-host")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")

    // Exposed Dependenecies
    implementation("org.jetbrains.exposed:exposed-core:${exposedVersion}") // foundational database components
    implementation("org.jetbrains.exposed:exposed-jdbc:${exposedVersion}") // Java database connectivity support
    implementation("org.jetbrains.exposed:exposed-dao:1.1.1") // note sure if necessary, optional data access model

    // SQLite dependency Source: https://mvnrepository.com/artifact/org.xerial/sqlite-jdbc
    implementation("org.xerial:sqlite-jdbc:$sqliteVersion")


}