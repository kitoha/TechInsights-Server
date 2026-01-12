package com.techinsights.domain.config.resilience

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
@EnableConfigurationProperties(RateLimiterProperties::class, CircuitBreakerProperties::class)
class ResilienceConfig(
  private val rateLimiterProperties: RateLimiterProperties,
  private val circuitBreakerProperties: CircuitBreakerProperties
) {

  @Bean
  fun rateLimiterRegistry(): RateLimiterRegistry {
    val geminiConfig = createRateLimiterConfig(rateLimiterProperties.gemini)
    val registry = RateLimiterRegistry.of(geminiConfig)

    val geminiBatchConfig = createRateLimiterConfig(rateLimiterProperties.geminiBatch)
    val conservativeConfig = createRateLimiterConfig(rateLimiterProperties.crawler.conservative)
    val defaultConfig = createRateLimiterConfig(rateLimiterProperties.crawler.default)

    registry.rateLimiter("geminiArticleSummarizer", geminiConfig)
    registry.rateLimiter("geminiBatchSummarizer", geminiBatchConfig)
    registry.rateLimiter("woowahan", conservativeConfig)
    registry.rateLimiter("gccompany", conservativeConfig)
    registry.rateLimiter("defaultCrawler", defaultConfig)

    return registry
  }

  @Bean
  fun circuitBreakerRegistry(): CircuitBreakerRegistry {
    val registry = CircuitBreakerRegistry.ofDefaults()

    val batchProps = circuitBreakerProperties.geminiBatch
    val batchConfig = CircuitBreakerConfig.custom()
      .failureRateThreshold(batchProps.failureRateThreshold.toFloat())
      .slowCallRateThreshold(batchProps.slowCallRateThreshold.toFloat())
      .slowCallDurationThreshold(Duration.ofSeconds(batchProps.slowCallDurationThresholdSeconds))
      .waitDurationInOpenState(Duration.ofSeconds(batchProps.waitDurationInOpenStateSeconds))
      .permittedNumberOfCallsInHalfOpenState(batchProps.permittedNumberOfCallsInHalfOpenState)
      .slidingWindowSize(batchProps.slidingWindowSize)
      .minimumNumberOfCalls(batchProps.minimumNumberOfCalls)
      .build()

    registry.circuitBreaker("geminiBatch", batchConfig)

    return registry
  }

  private fun createRateLimiterConfig(config: RateLimiterProperties.LimiterConfig): RateLimiterConfig {
    return RateLimiterConfig.custom()
      .limitForPeriod(config.limitForPeriod)
      .limitRefreshPeriod(Duration.ofSeconds(config.refreshPeriodSeconds))
      .timeoutDuration(Duration.ofSeconds(config.timeoutSeconds))
      .build()
  }
}