package com.techinsights.batch.parser.feed

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class BaseFeedStrategyTest : FunSpec({

  class TestFeedStrategy : BaseFeedStrategy() {

    override fun supports(document: Document) = true
    override fun parseEntries(document: Document) = emptyList<Element>()
    override fun extractLink(element: Element) = ""
    override fun extractPublishedDate(element: Element) = ""
    override fun extractContent(element: Element) = ""

    fun testExtractStructuredText(html: String) = extractStructuredText(html)
  }

  val strategy = TestFeedStrategy()

  test("헤더 태그를 올바르게 추출한다") {
    val html = """
            <h1>Main Title</h1>
            <h2>Subtitle</h2>
            <h3>Section</h3>
        """.trimIndent()

    val result = strategy.testExtractStructuredText(html)

    result shouldContain "Main Title"
    result shouldContain "Subtitle"
    result shouldContain "Section"
  }

  test("단락을 올바르게 추출한다") {
    val html = """
            <p>First paragraph.</p>
            <p>Second paragraph.</p>
        """.trimIndent()

    val result = strategy.testExtractStructuredText(html)

    result shouldContain "First paragraph"
    result shouldContain "Second paragraph"
  }

  test("리스트 아이템을 올바르게 추출한다") {
    val html = """
            <ul>
                <li>Item 1</li>
                <li>Item 2</li>
                <li>Item 3</li>
            </ul>
        """.trimIndent()

    val result = strategy.testExtractStructuredText(html)

    result shouldContain "Item 1"
    result shouldContain "Item 2"
    result shouldContain "Item 3"
  }

  test("blockquote를 올바르게 추출한다") {
    val html = """<blockquote>This is a quote.</blockquote>"""

    val result = strategy.testExtractStructuredText(html)

    result shouldContain "This is a quote"
  }

  test("링크 텍스트를 추출한다") {
    val html = """<p>Visit <a href="https://example.com">our website</a> for more.</p>"""

    val result = strategy.testExtractStructuredText(html)

    result shouldContain "our website"
  }

  test("중첩된 컨테이너 태그를 올바르게 처리한다") {
    val html = """
            <div>
                <section>
                    <article>
                        <h2>Nested Title</h2>
                        <p>Nested content</p>
                    </article>
                </section>
            </div>
        """.trimIndent()

    val result = strategy.testExtractStructuredText(html)

    result shouldContain "Nested Title"
    result shouldContain "Nested content"
  }

  test("3개 이상의 연속된 개행을 2개로 줄인다") {
    val html = """
            <p>First</p>
            <p>Second</p>
            <p>Third</p>
        """.trimIndent()

    val result = strategy.testExtractStructuredText(html)

    result shouldNotContain "\n\n\n"
  }

  test("앞뒤 공백을 제거한다") {
    val html = """<p>Content</p>"""

    val result = strategy.testExtractStructuredText(html)

    result shouldBe result.trim()
  }

  test("script와 style 태그는 무시한다") {
    val html = """
            <p>Visible content</p>
            <script>alert('test');</script>
            <style>body { color: red; }</style>
        """.trimIndent()

    val result = strategy.testExtractStructuredText(html)

    result shouldContain "Visible content"
    result shouldNotContain "alert"
    result shouldNotContain "color: red"
  }

  test("복잡한 HTML 구조를 올바르게 처리한다") {
    val html = """
            <article>
                <h1>Article Title</h1>
                <p>Introduction paragraph with <a href="#">a link</a>.</p>
                <h2>Section 1</h2>
                <p>First section content.</p>
                <ul>
                    <li>Point 1</li>
                    <li>Point 2</li>
                </ul>
                <blockquote>Important quote here.</blockquote>
                <h2>Section 2</h2>
                <p>Second section content.</p>
            </article>
        """.trimIndent()

    val result = strategy.testExtractStructuredText(html)

    result shouldContain "Article Title"
    result shouldContain "Introduction paragraph"
    result shouldContain "a link"
    result shouldContain "Section 1"
    result shouldContain "Point 1"
    result shouldContain "Important quote"
  }
})