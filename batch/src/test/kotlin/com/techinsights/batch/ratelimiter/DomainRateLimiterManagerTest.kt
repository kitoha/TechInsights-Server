package com.techinsights.batch.ratelimiter

import com.techinsights.batch.config.JitterConfig
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
    lateinit var jitterConfig: JitterConfig
    lateinit var domainRateLimiterManager: DomainRateLimiterManager

    beforeEach {
        rateLimiterRegistry = mockk()
        jitterConfig = mockk(relaxed = true)
        domainRateLimiterManager = DomainRateLimiterManager(rateLimiterRegistry, jitterConfig)
    }

    test("techblog.woowahan.com requests conservative tier") {
        val url = "https://techblog.woowahan.com/1234/post"
        val expectedRateLimiter = mockk<RateLimiter>()

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

    test("tech.kakao.com requests standard tier") {
        val url = "https://tech.kakao.com/1234/post"
        val expectedRateLimiter = mockk<RateLimiter>()

        every {
            rateLimiterRegistry.rateLimiter(
                "tech.kakao.com-standard",
                any<Supplier<RateLimiterConfig>>()
            )
        } returns expectedRateLimiter

        val result = domainRateLimiterManager.getRateLimiter(url)

        result shouldBe expectedRateLimiter
    }

    test("unknown domain requests ultraSafe tier") {
        val url = "https://unknown-blog.com/post"
        val expectedRateLimiter = mockk<RateLimiter>()
        
        every { 
            rateLimiterRegistry.rateLimiter(
                "unknown-blog.com-ultraSafe", 
                any<Supplier<RateLimiterConfig>>()
            ) 
        } returns expectedRateLimiter

        val result = domainRateLimiterManager.getRateLimiter(url)

        result shouldBe expectedRateLimiter
    }

    test("applyJitter should do nothing when disabled") {
        every { jitterConfig.enabled } returns false
        
        domainRateLimiterManager.applyJitter()
    }

    test("applyJitter should sleep when enabled") {
        every { jitterConfig.enabled } returns true
        every { jitterConfig.minMs } returns 10
        every { jitterConfig.maxMs } returns 20
        
        domainRateLimiterManager.applyJitter()
    }

    test("extractDomain should return the string itself if no protocol found") {
        val url = "not-a-url"
        val expectedRateLimiter = mockk<RateLimiter>()

        every { 
            rateLimiterRegistry.rateLimiter(
                "not-a-url-ultraSafe", 
                any<Supplier<RateLimiterConfig>>()
            ) 
        } returns expectedRateLimiter

        val result = domainRateLimiterManager.getRateLimiter(url)
        result shouldBe expectedRateLimiter
    }
})
