package com.techinsights.batch.crawling.parser.content

import com.techinsights.batch.crawling.parser.content.http.HttpHeaderProvider
import com.techinsights.batch.crawling.parser.content.http.UserAgentPool
import com.techinsights.batch.crawling.ratelimiter.DomainRateLimiterManager
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URI

class WebContentExtractor(
    private val selectorRegistry: ContentSelectorRegistry,
    private val textExtractor: HtmlTextExtractor,
    private val rateLimiterManager: DomainRateLimiterManager,
    private val userAgentPool: UserAgentPool,
    private val httpHeaderProvider: HttpHeaderProvider,
    private val timeout: Int = 5000
) : ContentExtractor {

    override fun extract(url: String, fallbackContent: String): String {
        return runCatching {
            val domain = extractDomain(url)

            val rateLimiter = rateLimiterManager.getRateLimiter(url)
            val document = rateLimiter.executeSupplier {
                rateLimiterManager.applyJitter()
                fetchDocument(url, domain)
            }

            val selectors = selectorRegistry.getSelectors(domain)
            selectors.firstNotNullOfOrNull { selector ->
                document.selectFirst(selector)?.let { element ->
                    textExtractor.extract(element).takeIf { it.isNotBlank() }
                }
            } ?: fallbackContent
        }.getOrElse { fallbackContent }
    }

    private fun fetchDocument(url: String, domain: String): Document {
        val userAgent = userAgentPool.getUserAgent(domain)
        val headers = httpHeaderProvider.getRealisticHeaders(url, userAgent)

        return Jsoup.connect(url)
            .userAgent(userAgent)
            .headers(headers)
            .timeout(timeout)
            .get()
    }

    private fun extractDomain(url: String): String =
        runCatching { URI(url).host.removePrefix("www.") }
            .getOrDefault("")
}
