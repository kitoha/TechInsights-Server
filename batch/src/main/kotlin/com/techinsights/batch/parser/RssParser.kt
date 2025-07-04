package com.techinsights.batch.parser

import com.techinsights.batch.extract.CompositeThumbnailExtractor
import com.techinsights.batch.util.FeedParseUtil.extractFullContent
import com.techinsights.batch.util.FeedParseUtil.getSingleTagText
import com.techinsights.batch.util.FeedParseUtil.parseRssDate
import com.techinsights.domain.dto.company.CompanyDto
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.utils.Tsid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import org.springframework.stereotype.Component

@Component
class RssParser(
  private val thumbnailExtractor: CompositeThumbnailExtractor
) : BlogParser {

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
  )
  override fun supports(feedUrl: String): Boolean =
    feedUrl.endsWith(".rss") || feedUrl.endsWith(".xml") || feedUrl.contains("feed")

  override suspend fun parseList(companyDto: CompanyDto, content: String): List<PostDto> =
    withContext(Dispatchers.IO) {
      try {
        val document: Document = Jsoup.parse(content, "", Parser.xmlParser())
        val entryParser = FeedEntryParserResolver.resolve(document)

        entryParser.parseEntries(document).map { element ->
          val title = element.getSingleTagText("title")
          val url = element.getSingleTagText("link", "id")
          val contents = element.getSingleTagText("description", "content")
          val fullContent = extractFullContent(url, contents)
          val publishedAt = parseRssDate(element.getSingleTagText("pubDate", "updated", "published"))
          val thumbnail = extractBestThumbnail(element)

          PostDto(
            id = Tsid.generate(),
            title = title,
            url = url,
            content = fullContent,
            publishedAt = publishedAt,
            thumbnail = thumbnail,
            company = companyDto,
            viewCount = 0L,
            categories = emptySet()
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
      val doc = Jsoup.connect(url).get()
      thumbnailExtractor.extract(doc)
    }.getOrNull()
  }
}