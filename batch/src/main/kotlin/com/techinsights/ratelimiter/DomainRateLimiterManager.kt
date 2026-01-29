package com.techinsights.ratelimiter

import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class DomainRateLimiterManager(
    private val rateLimiterRegistry: RateLimiterRegistry
) {
    private val log = LoggerFactory.getLogger(javaClass)



    // Domain to Tier mapping
    private val domainTierMapping = mapOf(
        "medium.com" to "conservative",
        "techblog.woowahan.com" to "conservative",
        "tech.kakao.com" to "default",
        "toss.tech" to "default",
        "d2.naver.com" to "conservative",
        "hyperconnect.com" to "default"
    )

    fun getRateLimiter(url: String): RateLimiter {
        val domain = extractDomain(url)

        val tier = domainTierMapping[domain] ?: "default"
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