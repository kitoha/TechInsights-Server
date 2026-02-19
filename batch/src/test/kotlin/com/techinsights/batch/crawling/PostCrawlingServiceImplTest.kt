package com.techinsights.batch.crawling

import com.techinsights.batch.parser.BlogParser
import com.techinsights.batch.parser.BlogParserResolver
import com.techinsights.domain.dto.company.CompanyDto
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.utils.Tsid
import com.techinsights.batch.ratelimiter.DomainRateLimiterManager
import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldBeEmpty
import io.mockk.*
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.LocalDateTime

class PostCrawlingServiceImplTest : FunSpec({

  lateinit var webClient: WebClient
  lateinit var parserResolver: BlogParserResolver
  lateinit var rateLimiterManager: DomainRateLimiterManager
  lateinit var service: PostCrawlingServiceImpl

  lateinit var requestHeadersUriSpec: WebClient.RequestHeadersUriSpec<*>
  lateinit var requestHeadersSpec: WebClient.RequestHeadersSpec<*>
  lateinit var responseSpec: WebClient.ResponseSpec

  beforeEach {
    webClient = mockk()
    parserResolver = mockk()
    rateLimiterManager = mockk()
    service = PostCrawlingServiceImpl(webClient, parserResolver, rateLimiterManager)

    requestHeadersUriSpec = mockk()
    requestHeadersSpec = mockk()
    responseSpec = mockk()
  }

  afterEach {
    clearAllMocks()
  }

  test("RSS Feed를 성공적으로 크롤링하고 파싱한다") {
    val companyDto = CompanyDto(
      id = Tsid.encode(1L),
      name = "우아한형제들",
      blogUrl = "https://techblog.woowahan.com/feed",
      logoImageName = "test.png",
      rssSupported = true
    )

    val rssContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
                <channel>
                    <item>
                        <title>Test Post</title>
                        <link>https://techblog.woowahan.com/12345/</link>
                    </item>
                </channel>
            </rss>
        """.trimIndent()

    val expectedPosts = listOf(
      createTestPost(1L, "Post 1", "https://techblog.woowahan.com/12345/", companyDto)
    )

    val rateLimiter = createTestRateLimiter()
    val parser = mockk<BlogParser>()

    every { rateLimiterManager.getRateLimiter(companyDto.blogUrl) } returns rateLimiter
    every { webClient.get() } returns requestHeadersUriSpec
    every { requestHeadersUriSpec.uri(companyDto.blogUrl) } returns requestHeadersSpec
    every { requestHeadersSpec.retrieve() } returns responseSpec
    every { responseSpec.bodyToMono(any<ParameterizedTypeReference<String>>()) } returns Mono.just(
      rssContent
    )
    every { parserResolver.resolve(companyDto.blogUrl) } returns parser
    coEvery { parser.parseList(companyDto, rssContent) } returns expectedPosts

    val result = service.processCrawledData(companyDto)

    result shouldNotBe null
    result shouldHaveSize 1
    result[0].title shouldBe "Post 1"
    result[0].url shouldBe "https://techblog.woowahan.com/12345/"

    verify(exactly = 1) { parserResolver.resolve(companyDto.blogUrl) }
    coVerify(exactly = 1) { parser.parseList(companyDto, rssContent) }
  }

  test("빈 RSS Feed를 처리할 때 빈 리스트를 반환한다") {
    val companyDto = CompanyDto(
      id = Tsid.encode(1L),
      name = "Test Company",
      blogUrl = "https://test.com/feed",
      logoImageName = "test.png",
      rssSupported = false
    )

    val emptyRss = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
                <channel></channel>
            </rss>
        """.trimIndent()

    val rateLimiter = createTestRateLimiter()
    val parser = mockk<BlogParser>()

    every { rateLimiterManager.getRateLimiter(any()) } returns rateLimiter
    every { webClient.get() } returns requestHeadersUriSpec
    every { requestHeadersUriSpec.uri(any<String>()) } returns requestHeadersSpec
    every { requestHeadersSpec.retrieve() } returns responseSpec
    every { responseSpec.bodyToMono(any<ParameterizedTypeReference<String>>()) } returns Mono.just(
      emptyRss
    )
    every { parserResolver.resolve(any()) } returns parser
    coEvery { parser.parseList(any(), any()) } returns emptyList()

    val result = service.processCrawledData(companyDto)

    result.shouldBeEmpty()
  }

  test("WebClient 요청 실패 시 예외를 전파한다") {
    val companyDto = CompanyDto(
      id = Tsid.encode(1L),
      name = "Test Company",
      blogUrl = "https://test.com/feed",
      logoImageName = "test.png",
      rssSupported = false
    )

    val rateLimiter = createTestRateLimiter()

    every { rateLimiterManager.getRateLimiter(any()) } returns rateLimiter
    every { webClient.get() } returns requestHeadersUriSpec
    every { requestHeadersUriSpec.uri(any<String>()) } returns requestHeadersSpec
    every { requestHeadersSpec.retrieve() } returns responseSpec
    every { responseSpec.bodyToMono(any<ParameterizedTypeReference<String>>()) } returns
        Mono.error(
          WebClientResponseException.create(
            500,
            "Internal Server Error",
            HttpHeaders.EMPTY,
            ByteArray(0),
            null
          )
        )

    shouldThrow<WebClientResponseException> {
      service.processCrawledData(companyDto)
    }
  }

  test("429 Too Many Requests 에러 발생 시 예외를 전파한다") {
    val companyDto = CompanyDto(
      id = Tsid.encode(1L),
      name = "Test Company",
      blogUrl = "https://test.com/feed",
      logoImageName = "test.png",
      rssSupported = false
    )

    val rateLimiter = createTestRateLimiter()

    every { rateLimiterManager.getRateLimiter(any()) } returns rateLimiter
    every { webClient.get() } returns requestHeadersUriSpec
    every { requestHeadersUriSpec.uri(any<String>()) } returns requestHeadersSpec
    every { requestHeadersSpec.retrieve() } returns responseSpec
    every { responseSpec.bodyToMono(any<ParameterizedTypeReference<String>>()) } returns
        Mono.error(
          WebClientResponseException.create(
            429, "Too Many Requests", HttpHeaders.EMPTY,
            ByteArray(0), null
          )
        )

    shouldThrow<WebClientResponseException> {
      service.processCrawledData(companyDto)
    }
  }

  test("여러 포스트가 있는 RSS Feed를 올바르게 파싱한다") {
    val companyDto = CompanyDto(
      id = Tsid.encode(1L),
      name = "Test Company",
      blogUrl = "https://test.com/feed",
      logoImageName = "test.png",
      rssSupported = false
    )

    val rssContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
                <channel>
                    <item>
                        <title>Post 1</title>
                        <link>https://test.com/1/</link>
                    </item>
                    <item>
                        <title>Post 2</title>
                        <link>https://test.com/2/</link>
                    </item>
                    <item>
                        <title>Post 3</title>
                        <link>https://test.com/3/</link>
                    </item>
                </channel>
            </rss>
        """.trimIndent()

    val expectedPosts = listOf(
      createTestPost(1L, "Post 1", "https://test.com/1/", companyDto),
      createTestPost(2L, "Post 2", "https://test.com/2/", companyDto),
      createTestPost(3L, "Post 3", "https://test.com/3/", companyDto)
    )

    val rateLimiter = createTestRateLimiter()
    val parser = mockk<BlogParser>()

    every { rateLimiterManager.getRateLimiter(any()) } returns rateLimiter
    every { webClient.get() } returns requestHeadersUriSpec
    every { requestHeadersUriSpec.uri(any<String>()) } returns requestHeadersSpec
    every { requestHeadersSpec.retrieve() } returns responseSpec
    every { responseSpec.bodyToMono(any<ParameterizedTypeReference<String>>()) } returns Mono.just(
      rssContent
    )
    every { parserResolver.resolve(any()) } returns parser
    coEvery { parser.parseList(any(), any()) } returns expectedPosts

    val result = service.processCrawledData(companyDto)

    result shouldHaveSize 3
    result[0].title shouldBe "Post 1"
    result[1].title shouldBe "Post 2"
    result[2].title shouldBe "Post 3"
  }
})

private fun createTestRateLimiter(): RateLimiter {
  val config = RateLimiterConfig.custom()
    .limitForPeriod(100)
    .limitRefreshPeriod(Duration.ofMinutes(1))
    .timeoutDuration(Duration.ofSeconds(1))
    .build()

  return RateLimiter.of("test", config)
}

private fun createRestrictiveRateLimiter(): RateLimiter {
  val config = RateLimiterConfig.custom()
    .limitForPeriod(0)  // 요청 불가
    .limitRefreshPeriod(Duration.ofMinutes(1))
    .timeoutDuration(Duration.ofMillis(1))
    .build()

  return RateLimiter.of("restrictive", config)
}

private fun createTestPost(
  id: Long,
  title: String,
  url: String,
  company: CompanyDto
): PostDto {
  return PostDto(
    id = Tsid.encode(id),
    title = title,
    preview = null,
    url = url,
    content = "Test content",
    publishedAt = LocalDateTime.now(),
    thumbnail = null,
    company = company,
    viewCount = 0L,
    categories = emptySet()
  )
}
