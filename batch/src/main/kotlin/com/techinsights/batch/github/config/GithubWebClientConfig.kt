package com.techinsights.batch.github.config

import com.techinsights.batch.github.config.props.GithubApiProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.WebClient

@Configuration
@EnableConfigurationProperties(GithubApiProperties::class)
class GithubWebClientConfig(
    private val props: GithubApiProperties,
) {
    @Bean("githubWebClient")
    fun githubWebClient(): WebClient {
        val builder = WebClient.builder()
            .baseUrl(props.baseUrl)
            .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
            .defaultHeader("X-GitHub-Api-Version", "2022-11-28")

        if (props.token.isNotBlank()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer ${props.token}")
        }

        return builder.build()
    }
}
