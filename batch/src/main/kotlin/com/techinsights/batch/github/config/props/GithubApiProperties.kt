package com.techinsights.batch.github.config.props

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "github.api")
data class GithubApiProperties(
    val token: String = "",
    val baseUrl: String = "https://api.github.com",
    val perPage: Int = 100,
)
