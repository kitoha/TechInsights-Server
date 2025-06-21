package com.techinsights.batch.parser

import com.techinsights.batch.extract.CompositeThumbnailExtractor
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
        val imageEl = linkEl?.selectFirst("img[data-main-image]")
        val dateText = el.selectFirst(".PostList-module--date--21238")?.text()?.trim()
        val tagEls = el.select("div.PostList-module--tag--0b5cb span")

        PostDto(
          id = Tsid.generate(),
          title = el.selectFirst("h1.PostList-module--title--a2e55")?.text().orEmpty(),
          url = linkEl?.absUrl("href").orEmpty(),
          content = el.selectFirst("p.PostList-module--sub--424ed")?.text().orEmpty(),
          publishedAt = parseHtmlDate(dateText),
          thumbnail = extractBestThumbnail(linkEl?.absUrl("href").orEmpty()),
          company = companyDto
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