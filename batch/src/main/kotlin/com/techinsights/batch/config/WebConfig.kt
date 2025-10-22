package com.techinsights.batch.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.util.retry.Retry
import java.time.Duration

@Configuration
class WebConfig {

  @Bean
  fun webClient(): WebClient{
    return     WebClient.builder()
      .defaultHeader(
        "User-Agent",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
      )
      .exchangeStrategies(
        ExchangeStrategies.builder()
          .codecs { config ->
            config.defaultCodecs().maxInMemorySize(5 * 1024 * 1024) // 5MB
          }
          .build()
      ).filter(retryFilter())
      .build()
  }

  private fun retryFilter(): ExchangeFilterFunction {
    return ExchangeFilterFunction { request, next ->
      next.exchange(request)
        .retryWhen(
          Retry.backoff(3, Duration.ofSeconds(2))
            .filter { it is WebClientResponseException.TooManyRequests }
        )
    }
  }
}