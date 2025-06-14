package com.techinsights.batch.parser

import com.techinsights.batch.extract.CompositeThumbnailExtractor
import com.techinsights.batch.util.FeedParseUtil.parseHtmlDate
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.utils.Tsid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.springframework.stereotype.Component

@Component
class ElevenStBlogParser(
  private val thumbnailExtractor: CompositeThumbnailExtractor
) : BlogParser {

  override fun supports(feedUrl: String): Boolean =
    feedUrl.contains("11st")

  override suspend fun parseList(feedUrl: String, content: String): List<PostDto> = withContext(
    Dispatchers.IO
  ) {
    try {
      val document = Jsoup.parse(content, feedUrl)

      val posts = document.select("ul#post-list > li.post-item")
      posts.map { el ->
        val anchor = el.selectFirst("a")
        val url = anchor?.absUrl("href").orEmpty()
        val title = anchor?.selectFirst("h3.post-title")?.text().orEmpty()
        val description = anchor?.selectFirst("p.post-excerpt")?.text().orEmpty()
        val date = el.selectFirst("div.post-meta > p.post-date")?.text()?.trim()
        val tagElements = el.select("p.post-tags > a.tag")
        val tags = tagElements.map { it.text() }

        PostDto(
          id = Tsid.generate(),
          title = title,
          url = url,
          content = description,
          publishedAt = parseHtmlDate(date),
          thumbnail = extractBestThumbnail(url)
        )
      }.filter { it.title.isNotEmpty() && it.url.isNotEmpty() }
    } catch (e: Exception) {
      emptyList()
    }
  }

  fun extractBestThumbnail(detailUrl: String): String? {
    return runCatching {
      if (detailUrl.isBlank()) return@runCatching null
      val doc = org.jsoup.Jsoup.connect(detailUrl).get()
      thumbnailExtractor.extract(doc)
    }.getOrNull()
  }
}
