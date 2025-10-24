package com.techinsights.batch.parser

import com.techinsights.batch.extract.CompositeThumbnailExtractor
import com.techinsights.batch.util.FeedParseUtil.extractFullContent
import com.techinsights.batch.util.FeedParseUtil.parseHtmlDate
import com.techinsights.domain.dto.company.CompanyDto
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.utils.Tsid
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class ElevenStBlogParser(
  private val thumbnailExtractor: CompositeThumbnailExtractor,
  @Qualifier("ioDispatcher") private val ioDispatcher: CoroutineDispatcher
) : BlogParser {

  override fun supports(feedUrl: String): Boolean =
    feedUrl.contains("11st")

  override suspend fun parseList(companyDto: CompanyDto, content: String): List<PostDto> = withContext(
    ioDispatcher
  ) {
    try {
      val document = Jsoup.parse(content, companyDto.blogUrl)

      val posts = document.select("ul#post-list > li.post-item")
      posts.map { el ->
        val anchor = el.selectFirst("a")
        val url = anchor?.absUrl("href").orEmpty()
        val title = anchor?.selectFirst("h3.post-title")?.text().orEmpty()
        val contents = anchor?.selectFirst("p.post-excerpt")?.text().orEmpty()
        val date = el.selectFirst("div.post-meta > p.post-date")?.text()?.trim()
        val fullContent = extractFullContent(url, contents)

        PostDto(
          id = Tsid.generate(),
          title = title,
          preview = null,
          url = url,
          content = fullContent,
          publishedAt = parseHtmlDate(date),
          thumbnail = extractBestThumbnail(url),
          company = companyDto,
          viewCount = 0L,
          categories = emptySet()
        )
      }.filter { it.title.isNotEmpty() && it.url.isNotEmpty() }
    } catch (e: Exception) {
      emptyList()
    }
  }

  fun extractBestThumbnail(detailUrl: String): String? {
    return runCatching {
      if (detailUrl.isBlank()) return@runCatching null
      val doc = Jsoup.connect(detailUrl).get()
      thumbnailExtractor.extract(doc)
    }.getOrNull()
  }
}
