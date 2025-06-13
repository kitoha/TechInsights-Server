package com.techinsights.batch.parser

import com.techinsights.batch.extract.CompositeThumbnailExtractor
import com.techinsights.batch.util.FeedParseUtil.getSingleTagText
import com.techinsights.batch.util.FeedParseUtil.parseRssDate
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.utils.Tsid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

@Component
class RssParser(
  private val thumbnailExtractor: CompositeThumbnailExtractor
) : BlogParser {

  override fun supports(feedUrl: String): Boolean =
    feedUrl.endsWith(".rss") || feedUrl.endsWith(".xml") || feedUrl.contains("feed")

  override suspend fun parseList(feedUrl: String, content: String): List<PostDto> =
    withContext(Dispatchers.IO) {
      try {
        val document = DocumentBuilderFactory
          .newInstance()
          .newDocumentBuilder()
          .parse(InputSource(StringReader(content)))

        val entryParser = FeedEntryParserResolver.resolve(document)

        entryParser.parseEntries(document).map { element ->
          PostDto(
            id = Tsid.generate(),
            title = element.getSingleTagText("title"),
            url = element.getSingleTagText("link", "id"),
            content = element.getSingleTagText("description", "content"),
            publishedAt = parseRssDate(element.getSingleTagText("pubDate", "updated", "published")),
            thumbnail = extractBestThumbnail(element)
          )
        }
      } catch (e: Exception) {
        emptyList()
      }
    }

  private fun extractBestThumbnail(element: Element): String? {
    val feedThumbnail = thumbnailExtractor.extract(element)
    if (!feedThumbnail.isNullOrBlank()) return feedThumbnail

    val url = element.getSingleTagText("link", "id")
    return runCatching {
      val doc = org.jsoup.Jsoup.connect(url).get()
      thumbnailExtractor.extract(doc)
    }.getOrNull()
  }
}