package com.techinsights.batch.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebConfig {

  @Bean
  fun webClient(): WebClient{
    return     WebClient.builder()
      .exchangeStrategies(
        ExchangeStrategies.builder()
          .codecs { config ->
            config.defaultCodecs().maxInMemorySize(5 * 1024 * 1024) // 5MB
          }
          .build()
      )
      .build()
  }
}