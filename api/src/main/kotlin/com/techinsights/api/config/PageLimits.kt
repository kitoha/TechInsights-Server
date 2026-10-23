package com.techinsights.api.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer

/** The one page-size cap of the API. Manual `size` params use [MAX_PAGE_SIZE]; `Pageable` params are clamped by [PageLimits]. */
const val MAX_PAGE_SIZE = 100L // Long: used in jakarta.validation @Max

@Configuration
class PageLimits {

  @Bean
  fun pageableMaxSizeCustomizer(): PageableHandlerMethodArgumentResolverCustomizer =
    PageableHandlerMethodArgumentResolverCustomizer { it.setMaxPageSize(MAX_PAGE_SIZE.toInt()) }
}
