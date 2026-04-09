package com.techinsights.batch.github.community.config

import com.techinsights.batch.github.community.config.props.CommunityApiProperties
import com.techinsights.batch.infrastructure.config.props.WebClientProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient

@Configuration
@EnableConfigurationProperties(WebClientProperties::class, CommunityApiProperties::class)
class CommunityWebClientConfig(
    private val webClientProperties: WebClientProperties,
    private val communityApiProperties: CommunityApiProperties,
) {
    @Bean("hnWebClient")
    fun hnWebClient(): WebClient = WebClient.builder()
        .baseUrl(communityApiProperties.hn.baseUrl)
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader(HttpHeaders.USER_AGENT, communityApiProperties.hn.userAgent)
        .exchangeStrategies(exchangeStrategies())
        .build()

    @Bean("redditWebClient")
    fun redditWebClient(): WebClient = WebClient.builder()
        .baseUrl(communityApiProperties.reddit.baseUrl)
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader(HttpHeaders.USER_AGENT, communityApiProperties.reddit.userAgent)
        .exchangeStrategies(exchangeStrategies())
        .build()

    private fun exchangeStrategies() = ExchangeStrategies.builder()
        .codecs { it.defaultCodecs().maxInMemorySize(webClientProperties.maxInMemorySizeBytes) }
        .build()
}
