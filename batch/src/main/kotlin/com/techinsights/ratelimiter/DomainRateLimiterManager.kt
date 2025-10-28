package com.techinsights.ratelimiter

import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class DomainRateLimiterManager(
  private val rateLimiterRegistry: RateLimiterRegistry
) {

  private val log = LoggerFactory.getLogger(DomainRateLimiterManager::class.java)

  private val domainMapping = mapOf(
    "techblog.woowahan.com" to "woowahan",
    "techblog.gccompany.co.kr" to "gccompany"
  )

  fun getRateLimiter(url: String): RateLimiter {
    val domain = extractDomain(url)
    val instanceName = domainMapping[domain] ?: "defaultCrawler"

    log.debug("Using RateLimiter [{}] for domain [{}]", instanceName, domain)

    return rateLimiterRegistry.rateLimiter(instanceName)
  }

  private fun extractDomain(url: String): String {
    return url.substringAfter("://")
      .substringBefore("/")
      .substringBefore(":")
  }
}