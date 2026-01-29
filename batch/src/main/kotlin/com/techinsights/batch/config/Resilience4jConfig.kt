package com.techinsights.batch.config

import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
class Resilience4jConfig {

    @Bean
    fun rateLimiterRegistry(): RateLimiterRegistry {
        val registry = RateLimiterRegistry.ofDefaults()

        val conservativeConfig = RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofSeconds(60))
            .limitForPeriod(10)
            .timeoutDuration(Duration.ofSeconds(300))
            .build()
        registry.addConfiguration("conservative", conservativeConfig)

        val defaultConfig = RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofSeconds(60))
            .limitForPeriod(15)
            .timeoutDuration(Duration.ofSeconds(300))
            .build()
        registry.addConfiguration("default", defaultConfig)

        val ultraSafeConfig = RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofSeconds(60))
            .limitForPeriod(5)
            .timeoutDuration(Duration.ofSeconds(300))
            .build()
        registry.addConfiguration("ultraSafe", ultraSafeConfig)

        return registry
    }
}
