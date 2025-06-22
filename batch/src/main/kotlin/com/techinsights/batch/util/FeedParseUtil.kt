package com.techinsights.batch.util

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.net.URI
import java.time.LocalDateTime
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object FeedParseUtil {

  private val contentSelectorMap = mapOf(
    "techblog.woowahan.com"     to ".post-content-inner > .post-content-body",
    "tech.kakao.com"            to ".inner_content > .daum-wm-content.preview",
    "toss.tech"                 to "article.css-hvd0pt > div.css-1vn47db",
    "d2.naver.com"              to ".post-area, .section-content, .post-body",
    "techblog.lycorp.co.jp"     to "article.bui_component > div.post_content_wrap > div.content_inner > div.content",
    "blog.banksalad.com"        to "div[class^=postDetailsstyle__PostDescription]",
    "aws.amazon.com"            to "article.blog-post section.blog-post-content[property=articleBody]",
    "hyperconnect.github.io"    to "article.post .post-content.e-content",
    "helloworld.kurly.com"      to ".post-content, .article-body, .post",
    "tech.socarcorp.kr"         to ".post-content, .article-body, .post",
    "dev.gmarket.com"           to ".post-content, .article-body, .post",
    "medium.com"                to "article, .meteredContent, .pw-post-body, .postArticle-content",
    "oliveyoung.tech"           to "div.blog-post-content"
  )

  fun Element.getSingleTagText(vararg tags: String): String {
    for (tag in tags) {
      this.selectFirst(tag)?.run {
        return when {
          hasAttr("href") -> attr("href")
          text().isNotBlank() -> text()
          else -> ""
        }
      }
    }
    return ""
  }

  fun parseRssDate(vararg dateStrings: String): LocalDateTime {
    for (dateString in dateStrings) {
      try {
        if (dateString.isNotBlank()) {
          val formatter = DateTimeFormatter.RFC_1123_DATE_TIME
          return ZonedDateTime.parse(dateString, formatter).toLocalDateTime()
        }
      } catch (ignored: Exception) {}
    }
    return LocalDateTime.now()
  }

  fun parseHtmlDate(dateText: String?): LocalDateTime {
    return try {
      LocalDate.parse(dateText, DateTimeFormatter.ofPattern("yyyy-MM-dd")).atStartOfDay()
    } catch (e: Exception) {
      LocalDateTime.now()
    }
  }

  fun extractStructuredText(element: Element?): String {
    if (element == null) return ""
    val sb = StringBuilder()

    fun traverse(node: Element) {
      for (child in node.children()) {
        when (child.tagName()) {
          "h1" -> sb.append("\n# ${child.text().trim()}\n")
          "h2" -> sb.append("\n## ${child.text().trim()}\n")
          "h3" -> sb.append("\n### ${child.text().trim()}\n")
          "li" -> sb.append("- ${child.text().trim()}\n")
          "p" -> sb.append("${child.text().trim()}\n")
          "ol", "ul", "div", "section", "article" -> traverse(child)
          else -> traverse(child)
        }
      }
    }
    traverse(element)
    return sb.toString().replace(Regex("\n{3,}"), "\n\n").trim()
  }

  fun extractFullContent(url: String, fallback: String): String {
    return try {
      val doc = Jsoup.connect(url)
        .userAgent("Mozilla/5.0 (compatible; TechInsightBot/1.0)")
        .timeout(5000)
        .get()
      val selector = contentSelectorMap[urlDomain(url)]
        ?: "article, .post-content, .entry-content, #content, .blog-post"
      val element = doc.selectFirst(selector)
      val structuredText = extractStructuredText(element)
      if (structuredText.isNotBlank()) structuredText else fallback
    } catch (e: Exception) {
      fallback
    }
  }

  private fun urlDomain(url: String): String =
    try { URI(url).host.removePrefix("www.") } catch (e: Exception) { "" }

}