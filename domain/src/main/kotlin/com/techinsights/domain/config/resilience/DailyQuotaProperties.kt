package com.techinsights.domain.config.resilience

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "daily-quota")
data class DailyQuotaProperties(
  val zone: String = "America/Los_Angeles"
)
