plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
    kotlin("kapt")
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
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("io.hypersistence:hypersistence-tsid:2.1.4")
    implementation("javax.annotation:javax.annotation-api:1.3.2")
    implementation("mysql:mysql-connector-java:8.0.33")
    implementation("com.querydsl:querydsl-core:5.1.0")
    implementation("com.querydsl:querydsl-jpa:5.1.0:jakarta")
    implementation("io.github.microutils:kotlin-logging:2.1.23")
    implementation("io.github.resilience4j:resilience4j-ratelimiter:2.3.0")
    implementation("com.google.genai:google-genai:1.6.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.awspring.cloud:spring-cloud-aws-starter-parameter-store:3.3.1")
    implementation ("software.amazon.awssdk:sso:2.31.70")
    implementation ("software.amazon.awssdk:ssooidc:2.31.70")
    kapt("com.querydsl:querydsl-apt:5.1.0:jakarta")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
    testImplementation("io.kotest:kotest-property:5.8.0")
    testImplementation("io.kotest.extensions:kotest-extensions-spring:1.1.3")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.mockk:mockk:1.13.8")
}

kotlin {
    jvmToolchain(21)
}

tasks {
    bootJar { enabled = false }
    jar { enabled = true }
}
