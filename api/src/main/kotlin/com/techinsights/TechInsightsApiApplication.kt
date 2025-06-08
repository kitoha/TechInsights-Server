package com.techinsights

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class TechInsightsApiApplication

fun main(args: Array<String>) {
    runApplication<TechInsightsApiApplication>(*args)
}
