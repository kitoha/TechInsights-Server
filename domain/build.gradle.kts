plugins {
  alias(libs.plugins.spring.boot)
  alias(libs.plugins.spring.dependency.management)
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.spring)
  alias(libs.plugins.kotlin.jpa)
  alias(libs.plugins.kotlin.kapt)
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

// Boot 3.5.0 manages Testcontainers 1.21.0, whose docker-java is rejected by Docker Engine 29 (min API 1.44)
extra["testcontainers.version"] = libs.versions.testcontainers.get()

dependencies {
  api(libs.resilience4j.ratelimiter)
  api(libs.resilience4j.circuitbreaker)
  api(libs.resilience4j.kotlin)
  api(libs.bundles.kotlinx.coroutines)

  implementation(libs.spring.boot.starter.data.jpa)
  implementation(libs.spring.boot.starter.cache)
  implementation(libs.caffeine)
  implementation(libs.flyway.core)
  implementation(libs.flyway.database.postgresql)
  implementation(libs.kotlin.reflect)
  implementation(libs.jackson.module.kotlin)
  implementation(libs.postgresql)
  implementation(libs.hibernate.vector)

  implementation("com.querydsl:querydsl-core:${libs.versions.querydsl.get()}")
  implementation("com.querydsl:querydsl-jpa:${libs.versions.querydsl.get()}:jakarta")
  kapt("com.querydsl:querydsl-apt:${libs.versions.querydsl.get()}:jakarta")

  implementation(libs.hypersistence.tsid)
  implementation(libs.javax.annotation.api)
  implementation(libs.kotlin.logging)
  implementation(libs.google.genai)

  testImplementation(libs.kotlin.test.junit5)
  testImplementation(libs.bundles.kotest)
  testImplementation(libs.spring.boot.starter.test)
  testImplementation(libs.mockk)
  testImplementation(libs.spring.boot.testcontainers)
  testImplementation(libs.testcontainers.postgresql)
}

kotlin {
  jvmToolchain(21)
}

tasks {
  bootJar { enabled = false }
  jar { enabled = true }
  test {
    // InlineFqnConventionTest scans the sources of every module, so they must invalidate the test task's up-to-date check
    inputs.files(fileTree("../api/src"), fileTree("../batch/src")).withPropertyName("conventionScannedSources")
  }
}