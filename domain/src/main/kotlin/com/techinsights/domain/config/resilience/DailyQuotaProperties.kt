package com.techinsights.domain.config.resilience

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "daily-quota")
data class DailyQuotaProperties(
  val zone: String = "America/Los_Angeles",
  val limits: Limits = Limits()
) {

  data class Limits(
    val batchSummary: Int = 20,
    val readmeSummary: Int = 20,
    val communityAnalysis: Int = 20
  )
}
