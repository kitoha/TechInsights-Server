package com.techinsights.ratelimiter

import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class DomainRateLimiterManagerTest : FunSpec({

  lateinit var rateLimiterRegistry: RateLimiterRegistry
  lateinit var domainRateLimiterManager: DomainRateLimiterManager

  beforeEach {
    rateLimiterRegistry = mockk()
    domainRateLimiterManager = DomainRateLimiterManager(rateLimiterRegistry)
  }

  test("매핑된 도메인에 대해 올바른 RateLimiter를 반환한다") {
    val url = "https://techblog.woowahan.com/1234/post"
    val expectedRateLimiter = mockk<RateLimiter>()
    every { rateLimiterRegistry.rateLimiter("woowahan") } returns expectedRateLimiter

    val result = domainRateLimiterManager.getRateLimiter(url)

    result shouldBe expectedRateLimiter
    verify { rateLimiterRegistry.rateLimiter("woowahan") }
  }

  test("매핑되지 않은 도메인에 대해 defaultCrawler RateLimiter를 반환한다") {
    val url = "https://unknown-blog.com/post"
    val defaultRateLimiter = mockk<RateLimiter>()
    every { rateLimiterRegistry.rateLimiter("defaultCrawler") } returns defaultRateLimiter

    val result = domainRateLimiterManager.getRateLimiter(url)

    result shouldBe defaultRateLimiter
    verify { rateLimiterRegistry.rateLimiter("defaultCrawler") }
  }

  test("포트 번호가 포함된 URL에서 도메인을 올바르게 추출한다") {
    val url = "http://techblog.woowahan.com:8080/post"
    val expectedRateLimiter = mockk<RateLimiter>()
    every { rateLimiterRegistry.rateLimiter("woowahan") } returns expectedRateLimiter

    val result = domainRateLimiterManager.getRateLimiter(url)

    result shouldBe expectedRateLimiter
    verify { rateLimiterRegistry.rateLimiter("woowahan") }
  }

  test("프로토콜이 없는 URL에서도 도메인을 올바르게 추출한다") {
    val url = "techblog.woowahan.com/post"
    val expectedRateLimiter = mockk<RateLimiter>()
    every { rateLimiterRegistry.rateLimiter("woowahan") } returns expectedRateLimiter

    val result = domainRateLimiterManager.getRateLimiter(url)

    result shouldBe expectedRateLimiter
    verify { rateLimiterRegistry.rateLimiter("woowahan") }
  }

  test("루트 경로만 있는 URL에서 도메인을 올바르게 추출한다") {
    val url = "https://techblog.woowahan.com/"
    val expectedRateLimiter = mockk<RateLimiter>()
    every { rateLimiterRegistry.rateLimiter("woowahan") } returns expectedRateLimiter

    val result = domainRateLimiterManager.getRateLimiter(url)

    result shouldBe expectedRateLimiter
    verify { rateLimiterRegistry.rateLimiter("woowahan") }
  }

  test("쿼리 파라미터가 포함된 URL에서 도메인을 올바르게 추출한다") {
    val url = "https://techblog.gccompany.co.kr/post?id=123&category=tech"
    val expectedRateLimiter = mockk<RateLimiter>()
    every { rateLimiterRegistry.rateLimiter("gccompany") } returns expectedRateLimiter

    val result = domainRateLimiterManager.getRateLimiter(url)

    result shouldBe expectedRateLimiter
    verify { rateLimiterRegistry.rateLimiter("gccompany") }
  }
})