package com.techinsights.batch.parser

import com.techinsights.batch.extract.CompositeThumbnailExtractor
import com.techinsights.batch.parser.content.ContentExtractor
import com.techinsights.batch.parser.date.CompositeDateParser
import com.techinsights.batch.parser.feed.FeedTypeStrategyResolver
import com.techinsights.batch.parser.feed.RssFeedStrategy
import com.techinsights.domain.dto.company.CompanyDto
import com.techinsights.domain.utils.Tsid
import com.techinsights.ratelimiter.DomainRateLimiterManager
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import org.springframework.web.reactive.function.client.WebClient
import java.time.LocalDateTime

class FeedParserTest : FunSpec({

  val thumbnailExtractor = mockk<CompositeThumbnailExtractor>()
  val feedTypeResolver = mockk<FeedTypeStrategyResolver>()
  val dateParser = mockk<CompositeDateParser>()
  val contentExtractor = mockk<ContentExtractor>()
  val webClient = mockk<WebClient>()
  val rateLimiterManager = mockk<DomainRateLimiterManager>()
  val parser = FeedParser(
    thumbnailExtractor,
    feedTypeResolver,
    dateParser,
    contentExtractor,
    rateLimiterManager,
    webClient,
    Dispatchers.Unconfined
  )

  beforeTest {
    clearAllMocks()
  }

  test("RSS 피드 URL을 지원한다") {
    parser.supports("https://example.com/feed.xml") shouldBe true
    parser.supports("https://example.com/rss") shouldBe true
    parser.supports("https://example.com/feed") shouldBe true
  }

  test("Atom 피드 URL을 지원한다") {
    parser.supports("https://example.com/atom.xml") shouldBe true
    parser.supports("https://example.com/feed.atom") shouldBe true
  }

  test("피드가 아닌 URL은 지원하지 않는다") {
    parser.supports("https://example.com/blog") shouldBe false
    parser.supports("https://example.com/posts") shouldBe false
  }

  test("RSS 피드를 파싱한다") {
    val rssXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
                <channel>
                    <item>
                        <title>First Post</title>
                        <link>https://example.com/post1</link>
                        <description>Description 1</description>
                        <pubDate>Mon, 01 Oct 2024 10:00:00 GMT</pubDate>
                    </item>
                    <item>
                        <title>Second Post</title>
                        <link>https://example.com/post2</link>
                        <description>Description 2</description>
                        <pubDate>Tue, 02 Oct 2024 10:00:00 GMT</pubDate>
                    </item>
                </channel>
            </rss>
        """.trimIndent()

    val companyDto = CompanyDto(
      id = Tsid.encode(3L),
      name = "Test Company",
      blogUrl = "https://example.com",
      logoImageName = "example.png",
      rssSupported = true
    )

    val strategy = RssFeedStrategy()
    every { feedTypeResolver.resolve(any()) } returns strategy
    every { dateParser.parse(any()) } returns LocalDateTime.now()
    every { contentExtractor.extract(any(), any()) } returns "Full content"
    every { thumbnailExtractor.extract(any<org.jsoup.nodes.Element>()) } returns null
    every { thumbnailExtractor.extract(any<org.jsoup.nodes.Document>()) } returns null

    val posts = parser.parseList(companyDto, rssXml)

    posts shouldHaveSize 2
    posts[0].title shouldBe "First Post"
    posts[0].url shouldBe "https://example.com/post1"
    posts[1].title shouldBe "Second Post"
    posts[1].url shouldBe "https://example.com/post2"
  }

  test("제목이 없는 엔트리는 무시한다") {
    val rssXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
                <channel>
                    <item>
                        <title>Valid Post</title>
                        <link>https://example.com/post1</link>
                        <description>Description</description>
                    </item>
                    <item>
                        <link>https://example.com/post2</link>
                        <description>No title</description>
                    </item>
                </channel>
            </rss>
        """.trimIndent()

    val companyDto = CompanyDto(
      id = Tsid.encode(3L),
      name = "Test Company",
      blogUrl = "https://example.com",
      logoImageName = "example.png",
      rssSupported = true
    )

    val strategy = RssFeedStrategy()
    every { feedTypeResolver.resolve(any()) } returns strategy
    every { dateParser.parse(any()) } returns LocalDateTime.now()
    every { contentExtractor.extract(any(), any()) } returns "Content"
    every { thumbnailExtractor.extract(any<org.jsoup.nodes.Element>()) } returns null

    val posts = parser.parseList(companyDto, rssXml)

    posts shouldHaveSize 1
    posts[0].title shouldBe "Valid Post"
  }

  test("파싱 중 에러가 발생하면 빈 리스트를 반환한다") {
    val invalidXml = "invalid xml content"
    val companyDto = CompanyDto(
      id = Tsid.encode(3L),
      name = "Test Company",
      blogUrl = "https://example.com",
      logoImageName = "example.png",
      rssSupported = true
    )

    every { feedTypeResolver.resolve(any()) } throws RuntimeException("Parse error")

    val posts = parser.parseList(companyDto, invalidXml)

    posts.shouldBeEmpty()
  }

  test("null 문자를 제거한다") {
    val rssXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
                <channel>
                    <item>
                        <title>Test${'\u0000'}Title</title>
                        <link>https://example.com/post1</link>
                        <description>Content</description>
                    </item>
                </channel>
            </rss>
        """.trimIndent()

    val companyDto = CompanyDto(
      id = Tsid.encode(3L),
      name = "Test Company",
      blogUrl = "https://example.com",
      logoImageName = "example.png",
      rssSupported = true
    )

    val strategy = RssFeedStrategy()
    every { feedTypeResolver.resolve(any()) } returns strategy
    every { dateParser.parse(any()) } returns LocalDateTime.now()
    every { contentExtractor.extract(any(), any()) } returns "Content\u0000"
    every { thumbnailExtractor.extract(any<org.jsoup.nodes.Element>()) } returns null

    val posts = parser.parseList(companyDto, rssXml)

    posts shouldHaveSize 1
    posts[0].title shouldBe "TestTitle"
    posts[0].content shouldBe "Content"
  }
})