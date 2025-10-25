package com.techinsights.batch.parser.feed

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.jsoup.Jsoup
import org.jsoup.parser.Parser

class RssFeedStrategyTest : FunSpec({

  val strategy = RssFeedStrategy()

  test("RSS 태그가 있으면 supports는 true를 반환한다") {
    val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
                <channel><title>Test Feed</title></channel>
            </rss>
        """.trimIndent()
    val doc = Jsoup.parse(xml, "", Parser.xmlParser())

    strategy.supports(doc) shouldBe true
  }

  test("channel 태그만 있어도 supports는 true를 반환한다") {
    val xml = """<channel><title>Test Feed</title></channel>"""
    val doc = Jsoup.parse(xml, "", Parser.xmlParser())

    strategy.supports(doc) shouldBe true
  }

  test("Atom 피드는 supports가 false를 반환한다") {
    val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <feed xmlns="http://www.w3.org/2005/Atom">
                <title>Test Feed</title>
            </feed>
        """.trimIndent()
    val doc = Jsoup.parse(xml, "", Parser.xmlParser())

    strategy.supports(doc) shouldBe false
  }

  test("parseEntries는 모든 item 엘리먼트를 반환한다") {
    val xml = """
            <rss>
                <channel>
                    <item><title>Article 1</title></item>
                    <item><title>Article 2</title></item>
                    <item><title>Article 3</title></item>
                </channel>
            </rss>
        """.trimIndent()
    val doc = Jsoup.parse(xml, "", Parser.xmlParser())

    val entries = strategy.parseEntries(doc)

    entries.size shouldBe 3
    entries[0].selectFirst("title")?.text() shouldBe "Article 1"
    entries[2].selectFirst("title")?.text() shouldBe "Article 3"
  }

  test("extractLink은 link 태그에서 링크를 추출한다") {
    val xml = """<item><link>https://example.com/article</link></item>"""
    val element = Jsoup.parse(xml, "", Parser.xmlParser()).selectFirst("item")!!

    strategy.extractLink(element) shouldBe "https://example.com/article"
  }

  test("extractLink은 link가 없으면 guid를 반환한다") {
    val xml = """<item><guid>https://example.com/article-guid</guid></item>"""
    val element = Jsoup.parse(xml, "", Parser.xmlParser()).selectFirst("item")!!

    strategy.extractLink(element) shouldBe "https://example.com/article-guid"
  }

  test("extractLink은 둘 다 없으면 빈 문자열을 반환한다") {
    val xml = """<item><title>Test</title></item>"""
    val element = Jsoup.parse(xml, "", Parser.xmlParser()).selectFirst("item")!!

    strategy.extractLink(element) shouldBe ""
  }

  test("extractPublishedDate는 pubDate 태그에서 날짜를 추출한다") {
    val xml = """
            <item>
                <pubDate>Wed, 02 Oct 2024 12:00:00 GMT</pubDate>
            </item>
        """.trimIndent()
    val element = Jsoup.parse(xml, "", Parser.xmlParser()).selectFirst("item")!!

    strategy.extractPublishedDate(element) shouldBe "Wed, 02 Oct 2024 12:00:00 GMT"
  }

  test("extractPublishedDate는 pubDate가 없으면 dc:date를 사용한다") {
    val xml = """
            <item xmlns:dc="http://purl.org/dc/elements/1.1/">
                <dc:date>2024-10-02T12:00:00Z</dc:date>
            </item>
        """.trimIndent()
    val element = Jsoup.parse(xml, "", Parser.xmlParser()).selectFirst("item")!!

    strategy.extractPublishedDate(element) shouldBe "2024-10-02T12:00:00Z"
  }

  test("extractPublishedDate는 둘 다 없으면 빈 문자열을 반환한다") {
    val xml = """<item><title>Test</title></item>"""
    val element = Jsoup.parse(xml, "", Parser.xmlParser()).selectFirst("item")!!

    strategy.extractPublishedDate(element) shouldBe ""
  }

  test("extractContent는 content:encoded 태그의 HTML을 파싱한다") {
    val xml = """
            <item xmlns:content="http://purl.org/rss/1.0/modules/content/">
                <content:encoded><![CDATA[
                    <h1>Main Title</h1>
                    <p>This is a paragraph.</p>
                    <ul>
                        <li>Item 1</li>
                        <li>Item 2</li>
                    </ul>
                ]]></content:encoded>
            </item>
        """.trimIndent()
    val element = Jsoup.parse(xml, "", Parser.xmlParser()).selectFirst("item")!!

    val content = strategy.extractContent(element)

    content shouldContain "Main Title"
    content shouldContain "This is a paragraph"
    content shouldContain "Item 1"
    content shouldContain "Item 2"
  }

  test("extractContent는 encoded 태그의 내용을 파싱한다") {
    val xml = """
            <item>
                <encoded>&lt;p&gt;Escaped HTML content&lt;/p&gt;</encoded>
            </item>
        """.trimIndent()
    val element = Jsoup.parse(xml, "", Parser.xmlParser()).selectFirst("item")!!

    val content = strategy.extractContent(element)

    content shouldContain "Escaped HTML content"
  }

  test("extractContent는 description 태그의 내용을 파싱한다") {
    val xml = """
            <item>
                <description>&lt;p&gt;Description content&lt;/p&gt;</description>
            </item>
        """.trimIndent()
    val element = Jsoup.parse(xml, "", Parser.xmlParser()).selectFirst("item")!!

    val content = strategy.extractContent(element)

    content shouldContain "Description content"
  }

  test("extractContent는 아무 태그도 없으면 빈 문자열을 반환한다") {
    val xml = """<item><title>Test</title></item>"""
    val element = Jsoup.parse(xml, "", Parser.xmlParser()).selectFirst("item")!!

    strategy.extractContent(element) shouldBe ""
  }
})
