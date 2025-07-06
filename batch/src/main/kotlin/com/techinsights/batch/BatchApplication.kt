package com.techinsights.batch

import org.springframework.boot.SpringApplication
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import kotlin.system.exitProcess

@SpringBootApplication
class BatchApplication {
}

fun main(args: Array<String>) {
  val context = SpringApplicationBuilder(BatchApplication::class.java)
    .web(WebApplicationType.NONE)
    .run(*args)

  val exitCode = SpringApplication.exit(context)

  exitProcess(exitCode)
}