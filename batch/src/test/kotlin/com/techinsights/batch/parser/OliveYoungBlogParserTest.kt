package com.techinsights.batch.parser

import com.techinsights.batch.extract.CompositeThumbnailExtractor
import com.techinsights.domain.dto.company.CompanyDto
import com.techinsights.domain.utils.Tsid
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.Dispatchers

class OliveYoungBlogParserTest : FunSpec({

  val thumbnailExtractor = mockk<CompositeThumbnailExtractor>()
  val parser = OliveYoungBlogParser(thumbnailExtractor, Dispatchers.Unconfined)

  beforeTest {
    clearAllMocks()
  }

  test("oliveyoung.tech를 포함한 URL을 지원한다") {
    parser.supports("https://oliveyoung.tech/blog") shouldBe true
    parser.supports("https://oliveyoung.tech/feed") shouldBe true
  }

  test("oliveyoung.tech를 포함하지 않은 URL은 지원하지 않는다") {
    parser.supports("https://example.com/feed") shouldBe false
    parser.supports("https://other-blog.com") shouldBe false
  }

  test("올리브영 블로그 HTML을 파싱한다") {
    val html = """
            <html>
                <body>
                    <ul>
                        <li class="PostList-module--item--95839">
                            <a class="PostList-module--wrapper--f39d6" href="/blog/post1">
                                <h1 class="PostList-module--title--a2e55">Test Post 1</h1>
                                <p class="PostList-module--sub--424ed">Preview content 1</p>
                                <span class="PostList-module--date--21238">2024-10-26</span>
                            </a>
                        </li>
                        <li class="PostList-module--item--95839">
                            <a class="PostList-module--wrapper--f39d6" href="/blog/post2">
                                <h1 class="PostList-module--title--a2e55">Test Post 2</h1>
                                <p class="PostList-module--sub--424ed">Preview content 2</p>
                                <span class="PostList-module--date--21238">2024-10-25</span>
                            </a>
                        </li>
                    </ul>
                </body>
            </html>
        """.trimIndent()

    val companyDto = CompanyDto(
      id = Tsid.encode(1L),
      name = "OliveYoung",
      blogUrl = "https://oliveyoung.tech",
      logoImageName = "oliveyoung.png",
      rssSupported = false,
      totalViewCount = 0L,
      postCount = 0L
    )

    every { thumbnailExtractor.extract(any()) } returns null
    mockkStatic("com.techinsights.batch.util.FeedParseUtil")

    val posts = parser.parseList(companyDto, html)

    posts shouldHaveSize 2
    posts[0].title shouldBe "Test Post 1"
    posts[1].title shouldBe "Test Post 2"

    unmockkStatic("com.techinsights.batch.util.FeedParseUtil")
  }

  test("빈 HTML을 처리하면 빈 리스트를 반환한다") {
    val html = "<html><body></body></html>"
    val companyDto = CompanyDto(
      id = Tsid.encode(1L),
      name = "OliveYoung",
      blogUrl = "https://oliveyoung.tech",
      logoImageName = "oliveyoung.png"
    )

    val posts = parser.parseList(companyDto, html)

    posts.shouldBeEmpty()
  }

  test("잘못된 HTML 형식이면 빈 리스트를 반환한다") {
    val invalidHtml = "invalid html content"
    val companyDto = CompanyDto(
      id = Tsid.encode(1L),
      name = "OliveYoung",
      blogUrl = "https://oliveyoung.tech",
      logoImageName = "oliveyoung.png"
    )

    val posts = parser.parseList(companyDto, invalidHtml)

    posts.shouldBeEmpty()
  }

  test("제목이나 URL이 비어있는 포스트는 필터링된다") {
    val html = """
            <html>
                <body>
                    <ul>
                        <li class="PostList-module--item--95839">
                            <a class="PostList-module--wrapper--f39d6" href="/blog/post1">
                                <h1 class="PostList-module--title--a2e55">Valid Post</h1>
                                <p class="PostList-module--sub--424ed">Content</p>
                            </a>
                        </li>
                        <li class="PostList-module--item--95839">
                            <a class="PostList-module--wrapper--f39d6" href="">
                                <h1 class="PostList-module--title--a2e55">No URL Post</h1>
                                <p class="PostList-module--sub--424ed">Content</p>
                            </a>
                        </li>
                        <li class="PostList-module--item--95839">
                            <a class="PostList-module--wrapper--f39d6" href="/blog/post3">
                                <h1 class="PostList-module--title--a2e55"></h1>
                                <p class="PostList-module--sub--424ed">Content</p>
                            </a>
                        </li>
                    </ul>
                </body>
            </html>
        """.trimIndent()

    val companyDto = CompanyDto(
      id = Tsid.encode(1L),
      name = "OliveYoung",
      blogUrl = "https://oliveyoung.tech",
      logoImageName = "oliveyoung.png"
    )

    every { thumbnailExtractor.extract(any()) } returns null
    mockkStatic("com.techinsights.batch.util.FeedParseUtil")

    val posts = parser.parseList(companyDto, html)

    posts shouldHaveSize 1
    posts[0].title shouldBe "Valid Post"

    unmockkStatic("com.techinsights.batch.util.FeedParseUtil")
  }
})