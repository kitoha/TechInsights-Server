package com.techinsights.domain.config.resilience

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "rate-limiter")
data class RateLimiterProperties(
  val gemini: LimiterConfig = LimiterConfig(
    limitForPeriod = 8,
    refreshPeriodSeconds = 60,
    timeoutSeconds = 30
  ),
  val geminiBatch: LimiterConfig = LimiterConfig(
    limitForPeriod = 20,
    refreshPeriodSeconds = 86400,
    timeoutSeconds = 60
  ),
  val crawler: CrawlerLimiterConfig = CrawlerLimiterConfig()
) {

  data class LimiterConfig(
    val limitForPeriod: Int,
    val refreshPeriodSeconds: Long,
    val timeoutSeconds: Long = 30
  )

  data class CrawlerLimiterConfig(
    val conservative: LimiterConfig = LimiterConfig(
      limitForPeriod = 15,
      refreshPeriodSeconds = 60,
      timeoutSeconds = 300
    ),
    val default: LimiterConfig = LimiterConfig(
      limitForPeriod = 20,
      refreshPeriodSeconds = 60,
      timeoutSeconds = 300
    )
  )
}