package com.techinsights.batch.crawling.config

import com.techinsights.batch.crawling.parser.feed.FeedTypeStrategyResolver
import com.techinsights.batch.crawling.parser.content.ContentExtractor
import com.techinsights.batch.crawling.parser.content.ContentSelectorRegistry
import com.techinsights.batch.crawling.parser.content.HtmlTextExtractor
import com.techinsights.batch.crawling.parser.content.WebContentExtractor
import com.techinsights.batch.crawling.parser.content.http.HttpHeaderProvider
import com.techinsights.batch.crawling.parser.content.http.UserAgentPool
import com.techinsights.batch.crawling.parser.date.CompositeDateParser
import com.techinsights.batch.crawling.ratelimiter.DomainRateLimiterManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FeedParserConfiguration {

  @Bean
  fun feedTypeStrategyResolver(): FeedTypeStrategyResolver =
    FeedTypeStrategyResolver()

  @Bean
  fun compositeDateParser(): CompositeDateParser =
    CompositeDateParser()

  @Bean
  fun htmlTextExtractor(): HtmlTextExtractor =
    HtmlTextExtractor()

  @Bean
  fun webContentExtractor(
    selectorRegistry: ContentSelectorRegistry,
    textExtractor: HtmlTextExtractor,
    rateLimiterManager: DomainRateLimiterManager,
    userAgentPool: UserAgentPool,
    httpHeaderProvider: HttpHeaderProvider
  ): ContentExtractor = WebContentExtractor(
    selectorRegistry,
    textExtractor,
    rateLimiterManager,
    userAgentPool,
    httpHeaderProvider
  )
}
