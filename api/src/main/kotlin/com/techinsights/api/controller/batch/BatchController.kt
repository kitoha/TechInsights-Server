package com.techinsights.api.controller.batch

import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/batch")
class BatchController(
  private val jobLauncher: JobLauncher,
  @Qualifier("crawlPostJob")
  private val crawlPostJob: Job
) {

  @PostMapping("/crawl-posts/run")
  fun runCrawlPostBatch(): ResponseEntity<String> {
    return try {
      val params = JobParametersBuilder()
        .addLong("time", System.currentTimeMillis())
        .toJobParameters()

      jobLauncher.run(crawlPostJob, params)
      ResponseEntity.ok("Batch started: crawlPostJob")
    } catch (e: Exception) {
      ResponseEntity.status(500).body("Batch 실행 실패: ${e.message}")
    }
  }
}
