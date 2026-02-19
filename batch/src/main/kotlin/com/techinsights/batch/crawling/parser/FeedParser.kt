package com.techinsights.batch.crawling.parser

import com.techinsights.batch.crawling.extract.CompositeThumbnailExtractor
import com.techinsights.batch.crawling.parser.content.ContentExtractor
import com.techinsights.batch.crawling.parser.date.CompositeDateParser
import com.techinsights.batch.crawling.parser.feed.FeedTypeStrategy
import com.techinsights.batch.crawling.parser.feed.FeedTypeStrategyResolver
import com.techinsights.domain.dto.company.CompanyDto
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.utils.Tsid
import com.techinsights.batch.crawling.ratelimiter.DomainRateLimiterManager
import io.github.resilience4j.kotlin.ratelimiter.executeSuspendFunction
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

@Component
class FeedParser(
  private val thumbnailExtractor: CompositeThumbnailExtractor,
  private val feedTypeResolver: FeedTypeStrategyResolver,
  private val dateParser: CompositeDateParser,
  private val contentExtractor: ContentExtractor,
  private val rateLimiterManager: DomainRateLimiterManager,
  private val webClient: WebClient,
  @Qualifier("ioDispatcher") private val ioDispatcher: CoroutineDispatcher
) : BlogParser {

  override fun supports(feedUrl: String): Boolean =
    feedUrl.endsWith(".xml") ||
        feedUrl.endsWith(".atom") ||
        feedUrl.contains("feed") ||
        feedUrl.contains("rss")


  override suspend fun parseList(companyDto: CompanyDto, content: String): List<PostDto> =
    withContext(ioDispatcher) {
      runCatching {
        val document = Jsoup.parse(content, "", Parser.xmlParser())
        val strategy = feedTypeResolver.resolve(document)

        strategy.parseEntries(document)
          .mapNotNull { element -> parseEntry(element, strategy, companyDto) }
      }.getOrDefault(emptyList())
    }

  private suspend fun parseEntry(
    element: Element,
    strategy: FeedTypeStrategy,
    companyDto: CompanyDto
  ): PostDto? {
    return runCatching {
      val title = element.selectFirst("title")?.text()?.sanitize() ?: return null
      val url = strategy.extractLink(element).takeIf { it.isNotBlank() } ?: return null
      val rawContent = strategy.extractContent(element)
      val fullContent = contentExtractor.extract(url, rawContent).sanitize()
      val publishedAt = dateParser.parse(strategy.extractPublishedDate(element))
      val thumbnail = extractThumbnail(element, url)

      PostDto(
        id = Tsid.generate(),
        title = title,
        preview = null,
        url = url,
        content = fullContent,
        publishedAt = publishedAt,
        thumbnail = thumbnail,
        company = companyDto,
        viewCount = 0L,
        categories = emptySet()
      )
    }.getOrNull()
  }

  private suspend fun extractThumbnail(element: Element, url: String): String? { // NOSONAR: 외부 의존성이라 테스트에서 제외
    thumbnailExtractor.extract(element)?.let { return it }

    return runCatching {
      val rateLimiter = rateLimiterManager.getRateLimiter(url)

      rateLimiter.executeSuspendFunction {
        val html = webClient.get()
          .uri(url)
          .retrieve()
          .awaitBody<String>()

        val doc = Jsoup.parse(html)
        thumbnailExtractor.extract(doc)
      }
    }.getOrNull()
  }

  private fun String.sanitize(): String {
    return this.replace("\u0000", "")
      .replace("\u0001", "")
      .trim()
  }
}
