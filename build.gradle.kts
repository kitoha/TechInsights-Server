import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
	alias(libs.plugins.kotlin.jvm) apply false
	alias(libs.plugins.kotlin.spring) apply false
	alias(libs.plugins.kotlin.jpa) apply false
	alias(libs.plugins.kotlin.kapt) apply false
	alias(libs.plugins.spring.boot) apply false
	alias(libs.plugins.spring.dependency.management) apply false
	alias(libs.plugins.sonarqube)
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
		if (buildFile.exists()) {
			tasks.withType<Test> {
				useJUnitPlatform()
			}

			tasks.matching { it.name == "jacocoTestReport" }.configureEach {
				if (this is JacocoReport) {
					dependsOn(tasks.withType<Test>())
					reports {
						xml.required.set(true)
						html.required.set(true)
					}

					classDirectories.setFrom(
						files(classDirectories.files.map {
							fileTree(it) {
								exclude(
									"**/config/**",
									"**/dto/**",
									"**/entity/**",
									"**/*Application*.class"
								)
							}
						})
					)
				}
			}
		}
	}
}

sonarqube {
	properties {
		property("sonar.projectKey", "kitoha_TechInsights-Server")
		property("sonar.organization", "kitoha")
		property("sonar.host.url", "https://sonarcloud.io")
		
		// Coverage report paths
		property(
			"sonar.coverage.jacoco.xmlReportPaths",
			"**/build/reports/jacoco/test/jacocoTestReport.xml"
		)

		// Coverage exclusions
		property(
			"sonar.coverage.exclusions",
			"**/config/**," +
					"**/dto/**," +
					"**/entity/**," +
					"**/*Application*.kt"
		)

		property(
			"sonar.exclusions",
			"**/*Application*.kt"
		)
	}
}