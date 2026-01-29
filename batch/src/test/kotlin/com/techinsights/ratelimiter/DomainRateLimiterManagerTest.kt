package com.techinsights.ratelimiter

import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.function.Supplier

class DomainRateLimiterManagerTest : FunSpec({

    lateinit var rateLimiterRegistry: RateLimiterRegistry
    lateinit var domainRateLimiterManager: DomainRateLimiterManager

    beforeEach {
        rateLimiterRegistry = mockk()
        domainRateLimiterManager = DomainRateLimiterManager(rateLimiterRegistry)
    }

    test("techblog.woowahan.com requests conservative tier") {
        val url = "https://techblog.woowahan.com/1234/post"
        val expectedRateLimiter = mockk<RateLimiter>()
        
        // Mocking the call with Supplier
        every { 
            rateLimiterRegistry.rateLimiter(
                "techblog.woowahan.com-conservative", 
                any<Supplier<RateLimiterConfig>>()
            ) 
        } returns expectedRateLimiter

        val result = domainRateLimiterManager.getRateLimiter(url)

        result shouldBe expectedRateLimiter
        verify { 
            rateLimiterRegistry.rateLimiter(
                "techblog.woowahan.com-conservative", 
                any<Supplier<RateLimiterConfig>>()
            ) 
        }
    }

    test("unknown domain requests default tier") {
        val url = "https://unknown-blog.com/post"
        val expectedRateLimiter = mockk<RateLimiter>()
        
        every { 
            rateLimiterRegistry.rateLimiter(
                "unknown-blog.com-default", 
                any<Supplier<RateLimiterConfig>>()
            ) 
        } returns expectedRateLimiter

        val result = domainRateLimiterManager.getRateLimiter(url)

        result shouldBe expectedRateLimiter
    }
})