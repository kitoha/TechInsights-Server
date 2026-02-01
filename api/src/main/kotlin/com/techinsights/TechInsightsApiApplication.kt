package com.techinsights

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class TechInsightsApiApplication

fun main(args: Array<String>) {
    runApplication<TechInsightsApiApplication>(*args)
}
