package com.techinsights.batch.crawling.parser.feed.handler

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import org.jsoup.Jsoup

class TagHandlerTest : FunSpec({

  context("Header") {
    test("레벨 1 헤더는 # 1개로 시작한다") {
      val html = "<h1>Main Title</h1>"
      val element = Jsoup.parse(html).selectFirst("h1")!!
      val sb = StringBuilder()

      TagHandler.Header(1).handle(element, sb)

      sb.toString() shouldBe "\n# Main Title\n\n"
    }

    test("레벨 2 헤더는 # 2개로 시작한다") {
      val html = "<h2>Subtitle</h2>"
      val element = Jsoup.parse(html).selectFirst("h2")!!
      val sb = StringBuilder()

      TagHandler.Header(2).handle(element, sb)

      sb.toString() shouldBe "\n## Subtitle\n\n"
    }

    test("레벨 6 헤더는 # 6개로 시작한다") {
      val html = "<h6>Small heading</h6>"
      val element = Jsoup.parse(html).selectFirst("h6")!!
      val sb = StringBuilder()

      TagHandler.Header(6).handle(element, sb)

      sb.toString() shouldBe "\n###### Small heading\n\n"
    }

    test("헤더 텍스트의 앞뒤 공백을 제거한다") {
      val html = "<h1>  Spaced Title  </h1>"
      val element = Jsoup.parse(html).selectFirst("h1")!!
      val sb = StringBuilder()

      TagHandler.Header(1).handle(element, sb)

      sb.toString() shouldBe "\n# Spaced Title\n\n"
    }

    test("중첩된 태그의 텍스트도 추출한다") {
      val html = "<h2>Title with <strong>bold</strong> text</h2>"
      val element = Jsoup.parse(html).selectFirst("h2")!!
      val sb = StringBuilder()

      TagHandler.Header(2).handle(element, sb)

      sb.toString() shouldBe "\n## Title with bold text\n\n"
    }
  }

  context("Paragraph") {
    test("단락 텍스트를 2개의 줄바꿈과 함께 추가한다") {
      val html = "<p>This is a paragraph.</p>"
      val element = Jsoup.parse(html).selectFirst("p")!!
      val sb = StringBuilder()

      TagHandler.Paragraph.handle(element, sb)

      sb.toString() shouldBe "This is a paragraph.\n\n"
    }

    test("앞뒤 공백을 제거한다") {
      val html = "<p>  Spaced paragraph  </p>"
      val element = Jsoup.parse(html).selectFirst("p")!!
      val sb = StringBuilder()

      TagHandler.Paragraph.handle(element, sb)

      sb.toString() shouldBe "Spaced paragraph\n\n"
    }

    test("빈 단락은 무시한다") {
      val html = "<p>   </p>"
      val element = Jsoup.parse(html).selectFirst("p")!!
      val sb = StringBuilder()

      TagHandler.Paragraph.handle(element, sb)

      sb.toString() shouldBe ""
    }

    test("중첩된 태그의 텍스트를 포함한다") {
      val html = "<p>Text with <em>emphasis</em> and <strong>bold</strong>.</p>"
      val element = Jsoup.parse(html).selectFirst("p")!!
      val sb = StringBuilder()

      TagHandler.Paragraph.handle(element, sb)

      sb.toString() shouldBe "Text with emphasis and bold.\n\n"
    }
  }

  context("ListItem") {
    test("리스트 아이템은 - 로 시작한다") {
      val html = "<li>First item</li>"
      val element = Jsoup.parse(html).selectFirst("li")!!
      val sb = StringBuilder()

      TagHandler.ListItem.handle(element, sb)

      sb.toString() shouldBe "- First item\n"
    }

    test("여러 리스트 아이템을 처리한다") {
      val html = """
                <ul>
                    <li>Item 1</li>
                    <li>Item 2</li>
                    <li>Item 3</li>
                </ul>
            """.trimIndent()
      val doc = Jsoup.parse(html)
      val sb = StringBuilder()

      doc.select("li").forEach { li ->
        TagHandler.ListItem.handle(li, sb)
      }

      sb.toString() shouldBe "- Item 1\n- Item 2\n- Item 3\n"
    }

    test("앞뒤 공백을 제거한다") {
      val html = "<li>  Spaced item  </li>"
      val element = Jsoup.parse(html).selectFirst("li")!!
      val sb = StringBuilder()

      TagHandler.ListItem.handle(element, sb)

      sb.toString() shouldBe "- Spaced item\n"
    }

    test("중첩된 태그의 텍스트를 포함한다") {
      val html = "<li>Item with <strong>bold</strong> text</li>"
      val element = Jsoup.parse(html).selectFirst("li")!!
      val sb = StringBuilder()

      TagHandler.ListItem.handle(element, sb)

      sb.toString() shouldBe "- Item with bold text\n"
    }
  }

  context("Blockquote") {
    test("인용구는 > 로 시작한다") {
      val html = "<blockquote>This is a quote.</blockquote>"
      val element = Jsoup.parse(html).selectFirst("blockquote")!!
      val sb = StringBuilder()

      TagHandler.Blockquote.handle(element, sb)

      sb.toString() shouldBe "> This is a quote.\n\n"
    }

    test("앞뒤 공백을 제거한다") {
      val html = "<blockquote>  Spaced quote  </blockquote>"
      val element = Jsoup.parse(html).selectFirst("blockquote")!!
      val sb = StringBuilder()

      TagHandler.Blockquote.handle(element, sb)

      sb.toString() shouldBe "> Spaced quote\n\n"
    }

    test("빈 인용구는 무시한다") {
      val html = "<blockquote>   </blockquote>"
      val element = Jsoup.parse(html).selectFirst("blockquote")!!
      val sb = StringBuilder()

      TagHandler.Blockquote.handle(element, sb)

      sb.toString() shouldBe ""
    }

    test("여러 줄의 인용구를 한 줄로 처리한다") {
      val html = """
                <blockquote>
                    First line
                    Second line
                    Third line
                </blockquote>
            """.trimIndent()
      val element = Jsoup.parse(html).selectFirst("blockquote")!!
      val sb = StringBuilder()

      TagHandler.Blockquote.handle(element, sb)

      sb.toString() shouldStartWith "> "
      sb.toString() shouldContain "First line"
      sb.toString() shouldContain "Second line"
    }
  }

  context("Link") {
    test("링크는 마크다운 형식으로 변환된다") {
      val html = """<a href="https://example.com">Example Site</a>"""
      val element = Jsoup.parse(html).selectFirst("a")!!
      val sb = StringBuilder()

      TagHandler.Link.handle(element, sb)

      sb.toString() shouldBe "[Example Site](https://example.com) "
    }

    test("상대 경로 링크도 처리한다") {
      val html = """<a href="/about">About Page</a>"""
      val element = Jsoup.parse(html).selectFirst("a")!!
      val sb = StringBuilder()

      TagHandler.Link.handle(element, sb)

      sb.toString() shouldBe "[About Page](/about) "
    }

    test("href가 없는 링크는 빈 URL로 처리한다") {
      val html = """<a>No href link</a>"""
      val element = Jsoup.parse(html).selectFirst("a")!!
      val sb = StringBuilder()

      TagHandler.Link.handle(element, sb)

      sb.toString() shouldBe "[No href link]() "
    }

    test("빈 텍스트의 링크는 무시한다") {
      val html = """<a href="https://example.com">   </a>"""
      val element = Jsoup.parse(html).selectFirst("a")!!
      val sb = StringBuilder()

      TagHandler.Link.handle(element, sb)

      sb.toString() shouldBe ""
    }

    test("앞뒤 공백을 제거한다") {
      val html = """<a href="https://example.com">  Spaced Link  </a>"""
      val element = Jsoup.parse(html).selectFirst("a")!!
      val sb = StringBuilder()

      TagHandler.Link.handle(element, sb)

      sb.toString() shouldBe "[Spaced Link](https://example.com) "
    }

    test("여러 링크를 연속으로 처리한다") {
      val html = """
                <p>
                    <a href="https://site1.com">Site 1</a>
                    <a href="https://site2.com">Site 2</a>
                </p>
            """.trimIndent()
      val doc = Jsoup.parse(html)
      val sb = StringBuilder()

      doc.select("a").forEach { link ->
        TagHandler.Link.handle(link, sb)
      }

      sb.toString() shouldBe "[Site 1](https://site1.com) [Site 2](https://site2.com) "
    }
  }

  context("Default") {
    test("자식이 없는 엘리먼트의 텍스트를 추가한다") {
      val html = "<span>Plain text</span>"
      val element = Jsoup.parse(html).selectFirst("span")!!
      val sb = StringBuilder()

      TagHandler.Default.handle(element, sb)

      sb.toString() shouldBe "Plain text "
    }

    test("자식이 있는 엘리먼트는 무시한다") {
      val html = "<div><p>Child paragraph</p></div>"
      val element = Jsoup.parse(html).selectFirst("div")!!
      val sb = StringBuilder()

      TagHandler.Default.handle(element, sb)

      sb.toString() shouldBe ""
    }

    test("빈 텍스트는 무시한다") {
      val html = "<span>   </span>"
      val element = Jsoup.parse(html).selectFirst("span")!!
      val sb = StringBuilder()

      TagHandler.Default.handle(element, sb)

      sb.toString() shouldBe ""
    }

    test("앞뒤 공백을 제거한다") {
      val html = "<span>  Spaced text  </span>"
      val element = Jsoup.parse(html).selectFirst("span")!!
      val sb = StringBuilder()

      TagHandler.Default.handle(element, sb)

      sb.toString() shouldBe "Spaced text "
    }

    test("여러 텍스트 노드를 처리한다") {
      val html = """
                <div>
                    <span>First</span>
                    <span>Second</span>
                    <span>Third</span>
                </div>
            """.trimIndent()
      val doc = Jsoup.parse(html)
      val sb = StringBuilder()

      doc.select("span").forEach { span ->
        TagHandler.Default.handle(span, sb)
      }

      sb.toString() shouldBe "First Second Third "
    }
  }

  context("통합 테스트") {
    test("다양한 핸들러를 조합하여 사용한다") {
      val html = """
                <article>
                    <h1>Article Title</h1>
                    <p>This is a paragraph with <a href="https://example.com">a link</a>.</p>
                    <ul>
                        <li>Item 1</li>
                        <li>Item 2</li>
                    </ul>
                    <blockquote>Important quote</blockquote>
                </article>
            """.trimIndent()

      val doc = Jsoup.parse(html)
      val sb = StringBuilder()

      TagHandler.Header(1).handle(doc.selectFirst("h1")!!, sb)
      TagHandler.Paragraph.handle(doc.selectFirst("p")!!, sb)
      doc.select("li").forEach { TagHandler.ListItem.handle(it, sb) }
      TagHandler.Blockquote.handle(doc.selectFirst("blockquote")!!, sb)

      val result = sb.toString()

      result shouldContain "# Article Title"
      result shouldContain "This is a paragraph"
      result shouldContain "- Item 1"
      result shouldContain "- Item 2"
      result shouldContain "> Important quote"
    }
  }
})
