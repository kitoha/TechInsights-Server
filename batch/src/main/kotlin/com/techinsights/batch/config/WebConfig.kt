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
  fun webClient(): WebClient {
    val httpClient = reactor.netty.http.client.HttpClient.create()
      .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
      .responseTimeout(Duration.ofSeconds(15))
      .doOnConnected { conn ->
        conn.addHandlerLast(io.netty.handler.timeout.ReadTimeoutHandler(15))
          .addHandlerLast(io.netty.handler.timeout.WriteTimeoutHandler(15))
      }

    return WebClient.builder()
      .clientConnector(org.springframework.http.client.reactive.ReactorClientHttpConnector(httpClient))
      .defaultHeader(
        "User-Agent",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
      )
      .defaultHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
      .defaultHeader("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7")
      .defaultHeader("Accept-Encoding", "gzip, deflate, br")
      .defaultHeader("Connection", "keep-alive")
      .defaultHeader("Upgrade-Insecure-Requests", "1")
      .exchangeStrategies(
        ExchangeStrategies.builder()
          .codecs { config ->
            config.defaultCodecs().maxInMemorySize(5 * 1024 * 1024) // 5MB
          }
          .build()
      )
      .filter(retryFilter())
      .build()
  }

  private fun retryFilter(): ExchangeFilterFunction {
    return ExchangeFilterFunction { request, next ->
      next.exchange(request)
        .retryWhen(
          Retry.backoff(3, Duration.ofSeconds(2))
            .filter { it is WebClientResponseException.TooManyRequests || it is WebClientResponseException.InternalServerError || it is WebClientResponseException.BadGateway || it is WebClientResponseException.ServiceUnavailable || it is WebClientResponseException.GatewayTimeout }
        )
    }
  }
}