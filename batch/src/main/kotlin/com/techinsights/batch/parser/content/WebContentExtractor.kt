package com.techinsights.batch.parser.content

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URI

class WebContentExtractor(
  private val selectorRegistry: ContentSelectorRegistry,
  private val textExtractor: HtmlTextExtractor,
  private val timeout: Int = 5000
) : ContentExtractor {

  override fun extract(url: String, fallbackContent: String): String {
    return runCatching {
      val domain = extractDomain(url)
      val document = fetchDocument(url)
      val selectors = selectorRegistry.getSelectors(domain)

      selectors.firstNotNullOfOrNull { selector ->
        document.selectFirst(selector)?.let { element ->
          textExtractor.extract(element).takeIf { it.isNotBlank() }
        }
      } ?: fallbackContent
    }.getOrElse { fallbackContent }
  }

  private fun fetchDocument(url: String): Document =
    Jsoup.connect(url)
      .userAgent("Mozilla/5.0 (compatible; TechInsightBot/1.0)")
      .timeout(timeout)
      .get()

  private fun extractDomain(url: String): String =
    runCatching { URI(url).host.removePrefix("www.") }
      .getOrDefault("")
}
