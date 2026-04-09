package com.techinsights.batch.github.community.config.props

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "community.api")
data class CommunityApiProperties(
    val hn: HnApiProperties = HnApiProperties(),
    val reddit: RedditApiProperties = RedditApiProperties(),
) {
    data class HnApiProperties(
        val baseUrl: String = "https://hn.algolia.com",
        val userAgent: String = "TechInsights-Bot/1.0",
    )

    data class RedditApiProperties(
        val baseUrl: String = "https://www.reddit.com",
        val userAgent: String = "TechInsights-Bot/1.0",
    )
}
