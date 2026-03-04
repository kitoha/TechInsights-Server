package com.techinsights.batch.github.config

import com.techinsights.batch.github.config.props.GithubApiProperties
import com.techinsights.batch.infrastructure.config.props.WebClientProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient

@Configuration
@EnableConfigurationProperties(GithubApiProperties::class, WebClientProperties::class)
class GithubWebClientConfig(
    private val props: GithubApiProperties,
    private val webClientProperties: WebClientProperties,
) {
    @Bean("githubWebClient")
    fun githubWebClient(): WebClient {
        val exchangeStrategies = ExchangeStrategies.builder()
            .codecs { config ->
                config.defaultCodecs().maxInMemorySize(webClientProperties.maxInMemorySizeBytes)
            }
            .build()

        val builder = WebClient.builder()
            .baseUrl(props.baseUrl)
            .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
            .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
            .exchangeStrategies(exchangeStrategies)

        if (props.token.isNotBlank()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer ${props.token}")
        }

        return builder.build()
    }
}
