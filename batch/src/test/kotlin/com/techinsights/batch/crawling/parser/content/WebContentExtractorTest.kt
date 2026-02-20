package com.techinsights.batch.crawling.parser.content

import com.techinsights.batch.crawling.parser.content.http.HttpHeaderProvider
import com.techinsights.batch.crawling.parser.content.http.UserAgentPool
import com.techinsights.batch.crawling.ratelimiter.DomainRateLimiterManager
import io.github.resilience4j.ratelimiter.RateLimiter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class WebContentExtractorTest : FunSpec({

    lateinit var selectorRegistry: ContentSelectorRegistry
    lateinit var textExtractor: HtmlTextExtractor
    lateinit var rateLimiterManager: DomainRateLimiterManager
    lateinit var userAgentPool: UserAgentPool
    lateinit var httpHeaderProvider: HttpHeaderProvider
    lateinit var webContentExtractor: WebContentExtractor

    beforeEach {
        selectorRegistry = mockk()
        textExtractor = mockk()
        rateLimiterManager = mockk()
        userAgentPool = mockk()
        httpHeaderProvider = mockk()
        webContentExtractor = WebContentExtractor(
            selectorRegistry,
            textExtractor,
            rateLimiterManager,
            userAgentPool,
            httpHeaderProvider
        )
        mockkStatic(Jsoup::class)
    }

    afterEach {
        unmockkStatic(Jsoup::class)
    }

    test("RateLimiter is acquired and realistic headers are used") {
        val url = "https://example.com/post"
        val domain = "example.com"
        val rateLimiter = mockk<RateLimiter>()
        val connection = mockk<Connection>(relaxed = true)
        val document = mockk<Document>()
        
        every { rateLimiterManager.getRateLimiter(url) } returns rateLimiter
        every { userAgentPool.getUserAgent(domain) } returns "Mozilla/Fake"
        every { httpHeaderProvider.getRealisticHeaders(url, "Mozilla/Fake") } returns mapOf("User-Agent" to "Mozilla/Fake")
        
        every { Jsoup.connect(url) } returns connection
        every { connection.userAgent("Mozilla/Fake") } returns connection
        every { connection.headers(any()) } returns connection
        every { connection.timeout(any()) } returns connection
        every { connection.get() } returns document
        
        every { rateLimiterManager.applyJitter() } returns Unit
        
        every { rateLimiter.executeSupplier(any<java.util.function.Supplier<*>>()) } answers { 
            (it.invocation.args[0] as java.util.function.Supplier<*>).get() as Document
        }

        every { selectorRegistry.getSelectors(domain) } returns listOf("article")
        every { document.selectFirst("article") } returns null // just end early

        val result = webContentExtractor.extract(url, "fallback")

        result shouldBe "fallback"
        
        verify { rateLimiterManager.getRateLimiter(url) }
        verify { rateLimiter.executeSupplier<Document>(any()) }
        verify { rateLimiterManager.applyJitter() }
        verify { Jsoup.connect(url) }
        verify { connection.userAgent("Mozilla/Fake") }
        verify { httpHeaderProvider.getRealisticHeaders(url, "Mozilla/Fake") }
    }
})
