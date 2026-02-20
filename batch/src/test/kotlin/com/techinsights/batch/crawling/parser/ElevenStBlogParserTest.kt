package com.techinsights.batch.crawling.parser

import com.techinsights.batch.crawling.extract.CompositeThumbnailExtractor
import com.techinsights.domain.dto.company.CompanyDto
import com.techinsights.domain.utils.Tsid
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.Dispatchers

class ElevenStBlogParserTest : FunSpec({

  val thumbnailExtractor = mockk<CompositeThumbnailExtractor>()
  val parser = ElevenStBlogParser(thumbnailExtractor, Dispatchers.Unconfined)

  beforeTest {
    clearAllMocks()
  }

  test("11st를 포함한 URL을 지원한다") {
    parser.supports("https://11st.tech/blog") shouldBe true
    parser.supports("https://blog.11st.co.kr") shouldBe true
  }

  test("11st를 포함하지 않은 URL은 지원하지 않는다") {
    parser.supports("https://example.com/feed") shouldBe false
    parser.supports("https://other-blog.com") shouldBe false
  }

  test("11번가 블로그 HTML을 파싱한다") {
    val html = """
            <html>
                <body>
                    <ul id="post-list">
                        <li class="post-item">
                            <a href="/post1">
                                <h3 class="post-title">First Post</h3>
                                <p class="post-excerpt">Excerpt 1</p>
                            </a>
                            <div class="post-meta">
                                <p class="post-date">2024-10-26</p>
                            </div>
                        </li>
                        <li class="post-item">
                            <a href="/post2">
                                <h3 class="post-title">Second Post</h3>
                                <p class="post-excerpt">Excerpt 2</p>
                            </a>
                            <div class="post-meta">
                                <p class="post-date">2024-10-25</p>
                            </div>
                        </li>
                    </ul>
                </body>
            </html>
        """.trimIndent()

    val companyDto = CompanyDto(
      id = Tsid.encode(2L),
      name = "11st",
      blogUrl = "https://11st.tech",
      logoImageName = "11st.png",
      rssSupported = false
    )

    every { thumbnailExtractor.extract(any()) } returns null
    mockkStatic("com.techinsights.batch.crawling.util.FeedParseUtil")

    val posts = parser.parseList(companyDto, html)

    posts shouldHaveSize 2
    posts[0].title shouldBe "First Post"
    posts[1].title shouldBe "Second Post"

    unmockkStatic("com.techinsights.batch.crawling.util.FeedParseUtil")
  }

  test("빈 HTML을 처리하면 빈 리스트를 반환한다") {
    val html = "<html><body></body></html>"
    val companyDto = CompanyDto(
      id = Tsid.encode(2L),
      name = "11st",
      blogUrl = "https://11st.tech",
      logoImageName = "11st.png"
    )

    val posts = parser.parseList(companyDto, html)

    posts.shouldBeEmpty()
  }

  test("제목이나 URL이 비어있는 포스트는 필터링된다") {
    val html = """
            <html>
                <body>
                    <ul id="post-list">
                        <li class="post-item">
                            <a href="/valid-post">
                                <h3 class="post-title">Valid Post</h3>
                                <p class="post-excerpt">Content</p>
                            </a>
                            <div class="post-meta">
                                <p class="post-date">2024-10-26</p>
                            </div>
                        </li>
                        <li class="post-item">
                            <a href="">
                                <h3 class="post-title">No URL</h3>
                                <p class="post-excerpt">Content</p>
                            </a>
                        </li>
                    </ul>
                </body>
            </html>
        """.trimIndent()

    val companyDto = CompanyDto(
      id = Tsid.encode(2L),
      name = "11st",
      blogUrl = "https://11st.tech",
      logoImageName = "11st.png"
    )

    every { thumbnailExtractor.extract(any()) } returns null
    mockkStatic("com.techinsights.batch.crawling.util.FeedParseUtil")

    val posts = parser.parseList(companyDto, html)

    posts shouldHaveSize 1
    posts[0].title shouldBe "Valid Post"

    unmockkStatic("com.techinsights.batch.crawling.util.FeedParseUtil")
  }
})