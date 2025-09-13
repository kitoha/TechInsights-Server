package com.techinsights.domain.config.resilience

import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
class ResilienceConfig {

  @Bean
  fun rateLimiterRegistry(): RateLimiterRegistry {
    val config = RateLimiterConfig.custom()
      .limitForPeriod(8)
      .limitRefreshPeriod(Duration.ofMinutes(1))
      .timeoutDuration(Duration.ofSeconds(30))
      .build()
    return RateLimiterRegistry.of(config)
  }
}