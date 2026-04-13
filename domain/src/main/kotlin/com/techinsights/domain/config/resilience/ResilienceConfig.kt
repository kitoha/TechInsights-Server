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

    val geminiBatchRpmConfig = createRateLimiterConfig(rateLimiterProperties.geminiBatchRpm)
    val geminiBatchRpdConfig = createRateLimiterConfig(rateLimiterProperties.geminiBatchRpd)
    val geminiEmbeddingConfig = createRateLimiterConfig(rateLimiterProperties.geminiEmbedding)
    val githubConfig = createRateLimiterConfig(rateLimiterProperties.github)
    val geminiReadmeRpmConfig = createRateLimiterConfig(rateLimiterProperties.geminiReadmeRpm)
    val geminiReadmeRpdConfig = createRateLimiterConfig(rateLimiterProperties.geminiReadmeRpd)
    val hnApiConfig = createRateLimiterConfig(rateLimiterProperties.hnApi)
    val redditApiConfig = createRateLimiterConfig(rateLimiterProperties.redditApi)
    val geminiCommunityRpmConfig = createRateLimiterConfig(rateLimiterProperties.geminiCommunityRpm)
    val geminiCommunityRpdConfig = createRateLimiterConfig(rateLimiterProperties.geminiCommunityRpd)
    val ultraSafeConfig = createRateLimiterConfig(rateLimiterProperties.crawler.ultraSafe)
    val conservativeConfig = createRateLimiterConfig(rateLimiterProperties.crawler.conservative)
    val defaultConfig = createRateLimiterConfig(rateLimiterProperties.crawler.standard)

    // 게시글 요약 관련 RateLimiter
    registry.rateLimiter("geminiArticleSummarizer", geminiConfig)
    registry.rateLimiter("geminiBatchRpm", geminiBatchRpmConfig)
    registry.rateLimiter("geminiBatchRpd", geminiBatchRpdConfig)
    registry.rateLimiter("geminiEmbedding", geminiEmbeddingConfig)

    // GitHub API RateLimiter
    registry.rateLimiter("githubApi", githubConfig)

    // README 요약 관련 RateLimiter
    registry.rateLimiter("geminiReadmeRpm", geminiReadmeRpmConfig)
    registry.rateLimiter("geminiReadmeRpd", geminiReadmeRpdConfig)

    // Community Insight RateLimiter
    registry.rateLimiter("hnApi", hnApiConfig)
    registry.rateLimiter("redditApi", redditApiConfig)
    registry.rateLimiter("geminiCommunityRpm", geminiCommunityRpmConfig)
    registry.rateLimiter("geminiCommunityRpd", geminiCommunityRpdConfig)

    // 크롤링 RateLimiter
    registry.addConfiguration("ultraSafe", ultraSafeConfig)
    registry.addConfiguration("conservative", conservativeConfig)
    registry.addConfiguration("standard", defaultConfig)

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

    val communityProps = circuitBreakerProperties.communityInsight
    val communityConfig = CircuitBreakerConfig.custom()
      .failureRateThreshold(communityProps.failureRateThreshold.toFloat())
      .slowCallRateThreshold(communityProps.slowCallRateThreshold.toFloat())
      .slowCallDurationThreshold(Duration.ofSeconds(communityProps.slowCallDurationThresholdSeconds))
      .waitDurationInOpenState(Duration.ofSeconds(communityProps.waitDurationInOpenStateSeconds))
      .permittedNumberOfCallsInHalfOpenState(communityProps.permittedNumberOfCallsInHalfOpenState)
      .slidingWindowSize(communityProps.slidingWindowSize)
      .minimumNumberOfCalls(communityProps.minimumNumberOfCalls)
      .build()

    registry.circuitBreaker("communityInsight", communityConfig)

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