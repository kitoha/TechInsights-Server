package com.techinsights.api.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.filter.ForwardedHeaderFilter

@Configuration
class AppConfig {
  @Bean
  fun forwardedHeaderFilter() = ForwardedHeaderFilter()
}