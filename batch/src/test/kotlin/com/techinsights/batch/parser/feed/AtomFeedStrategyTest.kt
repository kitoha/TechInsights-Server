package com.techinsights.batch.parser.feed

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.jsoup.Jsoup
import org.jsoup.parser.Parser

class AtomFeedStrategyTest : FunSpec({

  val strategy = AtomFeedStrategy()

  test("Atom 네임스페이스가 있는 feed 태그가 있으면 supports는 true를 반환한다") {
    val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <feed xmlns="http://www.w3.org/2005/Atom">
                <title>Test Feed</title>
            </feed>
        """.trimIndent()
    val doc = Jsoup.parse(xml, "", Parser.xmlParser())

    strategy.supports(doc) shouldBe true
  }

  test("RSS 피드는 supports가 false를 반환한다") {
    val xml = """<rss><channel><title>Test</title></channel></rss>"""
    val doc = Jsoup.parse(xml, "", Parser.xmlParser())

    strategy.supports(doc) shouldBe false
  }

  test("parseEntries는 모든 entry 엘리먼트를 반환한다") {
    val xml = """
            <feed xmlns="http://www.w3.org/2005/Atom">
                <entry><title>Entry 1</title></entry>
                <entry><title>Entry 2</title></entry>
            </feed>
        """.trimIndent()
    val doc = Jsoup.parse(xml, "", Parser.xmlParser())

    strategy.parseEntries(doc).size shouldBe 2
  }

  test("extractLink은 rel=alternate인 link를 우선으로 추출한다") {
    val xml = """
            <entry>
                <link href="https://example.com/other" rel="self"/>
                <link href="https://example.com/article" rel="alternate"/>
            </entry>
        """.trimIndent()
    val element = Jsoup.parse(xml, "", Parser.xmlParser()).selectFirst("entry")!!

    strategy.extractLink(element) shouldBe "https://example.com/article"
  }

  test("extractLink은 rel=alternate가 없으면 첫 번째 link를 사용한다") {
    val xml = """<entry><link href="https://example.com/first"/></entry>"""
    val element = Jsoup.parse(xml, "", Parser.xmlParser()).selectFirst("entry")!!

    strategy.extractLink(element) shouldBe "https://example.com/first"
  }

  test("extractLink은 link가 없으면 id를 사용한다") {
    val xml = """<entry><id>https://example.com/id</id></entry>"""
    val element = Jsoup.parse(xml, "", Parser.xmlParser()).selectFirst("entry")!!

    strategy.extractLink(element) shouldBe "https://example.com/id"
  }

  test("extractPublishedDate는 updated 태그를 우선으로 추출한다") {
    val xml = """
            <entry>
                <published>2024-10-01T12:00:00Z</published>
                <updated>2024-10-02T12:00:00Z</updated>
            </entry>
        """.trimIndent()
    val element = Jsoup.parse(xml, "", Parser.xmlParser()).selectFirst("entry")!!

    strategy.extractPublishedDate(element) shouldBe "2024-10-02T12:00:00Z"
  }

  test("extractPublishedDate는 updated가 없으면 published를 사용한다") {
    val xml = """<entry><published>2024-10-01T12:00:00Z</published></entry>"""
    val element = Jsoup.parse(xml, "", Parser.xmlParser()).selectFirst("entry")!!

    strategy.extractPublishedDate(element) shouldBe "2024-10-01T12:00:00Z"
  }

  test("extractContent는 content 태그의 HTML을 파싱하여 구조화된 텍스트를 반환한다") {
    val xml = """
            <entry>
                <content type="html">&lt;h2&gt;Title&lt;/h2&gt;&lt;p&gt;Content here&lt;/p&gt;</content>
            </entry>
        """.trimIndent()
    val element = Jsoup.parse(xml, "", Parser.xmlParser()).selectFirst("entry")!!

    val content = strategy.extractContent(element)

    content shouldContain "Title"
    content shouldContain "Content here"
  }

  test("extractContent는 content가 없으면 summary를 사용한다") {
    val xml = """<entry><summary>Summary text</summary></entry>"""
    val element = Jsoup.parse(xml, "", Parser.xmlParser()).selectFirst("entry")!!

    strategy.extractContent(element) shouldBe "Summary text"
  }
})
