package com.techinsights.batch.parser

import com.techinsights.batch.extract.CompositeThumbnailExtractor
import com.techinsights.batch.util.FeedParseUtil.extractFullContent
import com.techinsights.batch.util.FeedParseUtil.parseHtmlDate
import com.techinsights.domain.dto.company.CompanyDto
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.utils.Tsid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.springframework.stereotype.Component

@Component
class OliveYoungBlogParser(
  private val thumbnailExtractor: CompositeThumbnailExtractor
) : BlogParser {
  override fun supports(feedUrl: String): Boolean =
    feedUrl.contains("oliveyoung.tech")

  override suspend fun parseList(companyDto: CompanyDto, content: String): List<PostDto> = withContext(
    Dispatchers.IO) {
    try {
      val document = Jsoup.parse(content, companyDto.blogUrl)
      val posts = document.select("li.PostList-module--item--95839")
      posts.map { el ->
        val linkEl = el.selectFirst("a.PostList-module--wrapper--f39d6")
        val dateText = el.selectFirst(".PostList-module--date--21238")?.text()?.trim()
        val contents =  el.selectFirst("p.PostList-module--sub--424ed")?.text().orEmpty()
        val title = el.selectFirst("h1.PostList-module--title--a2e55")?.text().orEmpty()
        val url = linkEl?.absUrl("href").orEmpty()
        val fullContent = extractFullContent(url, contents)


        PostDto(
          id = Tsid.generate(),
          title = title,
          url = url,
          content = fullContent,
          publishedAt = parseHtmlDate(dateText),
          thumbnail = extractBestThumbnail(url),
          company = companyDto,
          viewCount = 0L,
          category = emptySet()
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