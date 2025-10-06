package com.techinsights.batch.config

import com.techinsights.batch.parser.content.ContentExtractor
import com.techinsights.batch.parser.content.ContentSelectorRegistry
import com.techinsights.batch.parser.content.HtmlTextExtractor
import com.techinsights.batch.parser.content.WebContentExtractor
import com.techinsights.batch.parser.date.CompositeDateParser
import com.techinsights.batch.parser.feed.FeedTypeStrategyResolver
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
  fun contentSelectorRegistry(): ContentSelectorRegistry =
    ContentSelectorRegistry()

  @Bean
  fun htmlTextExtractor(): HtmlTextExtractor =
    HtmlTextExtractor()

  @Bean
  fun webContentExtractor(
    selectorRegistry: ContentSelectorRegistry,
    textExtractor: HtmlTextExtractor
  ): ContentExtractor = WebContentExtractor(selectorRegistry, textExtractor)
}