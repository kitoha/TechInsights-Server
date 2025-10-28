package com.techinsights.domain.config.resilience

import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
@EnableConfigurationProperties(RateLimiterProperties::class)
class ResilienceConfig(
  private val properties: RateLimiterProperties
) {

  @Bean
  fun rateLimiterRegistry(): RateLimiterRegistry {
    val geminiConfig = createConfig(properties.gemini)
    val registry = RateLimiterRegistry.of(geminiConfig)

    val conservativeConfig = createConfig(properties.crawler.conservative)
    val defaultConfig = createConfig(properties.crawler.default)

    registry.rateLimiter("geminiArticleSummarizer", geminiConfig)
    registry.rateLimiter("woowahan", conservativeConfig)
    registry.rateLimiter("gccompany", conservativeConfig)
    registry.rateLimiter("defaultCrawler", defaultConfig)

    return registry
  }

  private fun createConfig(config: RateLimiterProperties.LimiterConfig): RateLimiterConfig {
    return RateLimiterConfig.custom()
      .limitForPeriod(config.limitForPeriod)
      .limitRefreshPeriod(Duration.ofSeconds(config.refreshPeriodSeconds))
      .timeoutDuration(Duration.ofSeconds(config.timeoutSeconds))
      .build()
  }
}