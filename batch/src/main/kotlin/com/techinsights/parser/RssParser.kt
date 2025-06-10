package com.techinsights.parser

import com.techinsights.dto.PostDto
import com.techinsights.utils.Tsid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.xml.parsers.DocumentBuilderFactory

@Component
class RssParser : BlogParser {

  override fun supports(feedUrl: String): Boolean =
    feedUrl.endsWith(".rss") || feedUrl.endsWith(".xml") || feedUrl.contains("feed")

  override suspend fun parseList(feedUrl: String, content: String): List<PostDto> = withContext(Dispatchers.IO) {
    try {
      val document = DocumentBuilderFactory
        .newInstance()
        .newDocumentBuilder()
        .parse(InputSource(StringReader(content)))

      val extractor = ThumbnailExtractorRegistry.extractorFor(feedUrl)

      val items = document.getElementsByTagName("item")
      (0 until items.length).map { index ->
        val item = items.item(index) as Element
        PostDto(
          id = Tsid.generate(),
          title = item.getSingleTagText("title"),
          url = item.getSingleTagText("link"),
          content = item.getSingleTagText("description"),
          publishedAt = parseRssDate(item.getSingleTagText("pubDate")),
          thumbnail = extractor.extractThumbnail(item) ?: PostHtmlThumbnailExtractor.extractFromUrl(
            item.getSingleTagText("link")
          )
        )
      }
    } catch (e: Exception) {
      emptyList()
    }
  }

  private fun Element.getSingleTagText(tag: String): String {
    val nodeList = this.getElementsByTagName(tag)
    return if (nodeList.length > 0) nodeList.item(0).textContent.trim() else ""
  }

  private fun Element.getCategoryList(): List<String> {
    val categories = this.getElementsByTagName("category")
    return (0 until categories.length).map { idx ->
      categories.item(idx).textContent.trim()
    }
  }

  private fun parseRssDate(dateString: String): LocalDateTime {
    return try {
      val formatter = DateTimeFormatter.RFC_1123_DATE_TIME
      ZonedDateTime.parse(dateString, formatter).toLocalDateTime()
    } catch (e: Exception) {
      LocalDateTime.now()
    }
  }
}