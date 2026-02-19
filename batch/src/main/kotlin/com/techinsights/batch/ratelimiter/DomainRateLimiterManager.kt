package com.techinsights.batch.ratelimiter

import com.techinsights.batch.config.JitterConfig
import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import kotlin.random.Random

@Component
class DomainRateLimiterManager(
    private val rateLimiterRegistry: RateLimiterRegistry,
    private val jitterConfig: JitterConfig
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun applyJitter() {
        if (jitterConfig.enabled) {
            val delay = Random.nextLong(jitterConfig.minMs, jitterConfig.maxMs)

            Thread.sleep(delay)
        }
    }

    private val domainTierMapping = mapOf(
        "medium.com" to "conservative",
        "techblog.woowahan.com" to "conservative",
        "tech.kakao.com" to "standard",
        "toss.tech" to "standard",
        "d2.naver.com" to "conservative",
        "hyperconnect.com" to "standard"
    )

    fun getRateLimiter(url: String): RateLimiter {
        val domain = extractDomain(url)

        val tier = domainTierMapping[domain] ?: "ultraSafe"
        val instanceName = "$domain-$tier"

        return rateLimiterRegistry.rateLimiter(instanceName) {
            rateLimiterRegistry.getConfiguration(tier)
                .orElse(rateLimiterRegistry.defaultConfig)
        }
    }

    private fun extractDomain(url: String): String {
        return try {
            url.substringAfter("://")
                .substringBefore("/")
                .substringBefore(":")
                .removePrefix("www.")
        } catch (e: Exception) {
            log.warn("Failed to extract domain from url: {}", url)
            "unknown"
        }
    }
}
