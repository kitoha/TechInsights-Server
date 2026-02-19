package com.techinsights.batch.infrastructure.config

import com.techinsights.batch.infrastructure.config.props.WebClientProperties
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.util.retry.Retry
import java.time.Duration

@Configuration
@org.springframework.boot.context.properties.EnableConfigurationProperties(WebClientProperties::class)
class WebConfig(
  private val properties: WebClientProperties
) {
  @Bean
  fun webClient(): WebClient {
    val httpClient = reactor.netty.http.client.HttpClient.create()
      .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.connectTimeoutMillis)
      .responseTimeout(Duration.ofSeconds(properties.responseTimeoutSeconds))
      .doOnConnected { conn ->
        conn.addHandlerLast(ReadTimeoutHandler(properties.readTimeoutSeconds.toInt()))
          .addHandlerLast(WriteTimeoutHandler(properties.writeTimeoutSeconds.toInt()))
      }

    return WebClient.builder()
      .clientConnector(ReactorClientHttpConnector(httpClient))
      .defaultHeader("User-Agent", properties.userAgent)
      .defaultHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
      .defaultHeader("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7")
      .defaultHeader("Accept-Encoding", "gzip, deflate, br")
      .defaultHeader("Connection", "keep-alive")
      .defaultHeader("Upgrade-Insecure-Requests", "1")
      .exchangeStrategies(
        ExchangeStrategies.builder()
          .codecs { config ->
            config.defaultCodecs().maxInMemorySize(properties.maxInMemorySizeBytes)
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
          Retry.backoff(properties.retry.maxAttempts, Duration.ofSeconds(properties.retry.backoffSeconds))
            .filter { it is WebClientResponseException.TooManyRequests || it is WebClientResponseException.InternalServerError || it is WebClientResponseException.BadGateway || it is WebClientResponseException.ServiceUnavailable || it is WebClientResponseException.GatewayTimeout }
        )
    }
  }
}