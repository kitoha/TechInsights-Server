package com.techinsights.batch.github.community.config

import com.techinsights.batch.github.community.config.props.CommunityApiProperties
import com.techinsights.batch.infrastructure.config.props.WebClientProperties
import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import java.time.Duration

@Configuration
@EnableConfigurationProperties(WebClientProperties::class, CommunityApiProperties::class)
class CommunityWebClientConfig(
    private val webClientProperties: WebClientProperties,
    private val communityApiProperties: CommunityApiProperties,
) {
    @Bean("hnWebClient")
    fun hnWebClient(): WebClient = WebClient.builder()
        .baseUrl(communityApiProperties.hn.baseUrl)
        .clientConnector(ReactorClientHttpConnector(httpClient()))
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader(HttpHeaders.USER_AGENT, communityApiProperties.hn.userAgent)
        .exchangeStrategies(exchangeStrategies())
        .build()

    @Bean("redditWebClient")
    fun redditWebClient(): WebClient = WebClient.builder()
        .baseUrl(communityApiProperties.reddit.baseUrl)
        .clientConnector(ReactorClientHttpConnector(httpClient()))
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader(HttpHeaders.USER_AGENT, communityApiProperties.reddit.userAgent)
        .exchangeStrategies(exchangeStrategies())
        .build()

    private fun httpClient(): HttpClient = HttpClient.create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, webClientProperties.connectTimeoutMillis)
        .responseTimeout(Duration.ofSeconds(webClientProperties.responseTimeoutSeconds))
        .doOnConnected { conn ->
            conn.addHandlerLast(ReadTimeoutHandler(webClientProperties.readTimeoutSeconds.toInt()))
                .addHandlerLast(WriteTimeoutHandler(webClientProperties.writeTimeoutSeconds.toInt()))
        }

    private fun exchangeStrategies() = ExchangeStrategies.builder()
        .codecs { it.defaultCodecs().maxInMemorySize(webClientProperties.maxInMemorySizeBytes) }
        .build()
}
