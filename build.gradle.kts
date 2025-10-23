import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
	kotlin("jvm") version "1.9.25" apply false
	kotlin("plugin.spring") version "1.9.25" apply false
	kotlin("plugin.jpa") version "1.9.25" apply false
	id("org.springframework.boot") version "3.5.0" apply false
	id("io.spring.dependency-management") version "1.1.7" apply false
	id("org.sonarqube") version "7.0.0.6105"
	jacoco
}

group = "com.techinsights"
version = "0.0.1-SNAPSHOT"

repositories {
	mavenCentral()
}

subprojects {
	apply(plugin = "jacoco")

	afterEvaluate {
		tasks.named<Test>("test") {
			useJUnitPlatform()
			finalizedBy("jacocoTestReport")
		}

		tasks.named<JacocoReport>("jacocoTestReport") {
			dependsOn("test")
			reports {
				xml.required.set(true)
				html.required.set(true)
			}
		}
	}
}

sonarqube {
	properties {
		property("sonar.projectKey", "kitoha_TechInsights-Server")
		property("sonar.organization", "kitoha")
		property("sonar.host.url", "https://sonarcloud.io")
		property(
			"sonar.coverage.jacoco.xmlReportPaths",
			"**/build/reports/jacoco/test/jacocoTestReport.xml"
		)
	}
}