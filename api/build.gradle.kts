plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.jpa)
}

group = "com.techinsights"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    implementation(project(":domain"))

    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.data.commons)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.micrometer.registry.prometheus)
    implementation(libs.kotlin.reflect)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.bundles.kotlinx.coroutines)
    implementation(libs.logstash.logback.encoder)
    implementation(libs.kotlin.logging)

    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.bundles.kotest)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.mockk)
}
kotlin {
    jvmToolchain(21)
}