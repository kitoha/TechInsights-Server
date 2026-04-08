package com.techinsights.batch.github.community.dto

import com.fasterxml.jackson.annotation.JsonProperty

// reddit
data class RedditSearchResponse(
    val data: RedditData? = null,
) {
    data class RedditData(
        val children: List<RedditChild> = emptyList(),
    )

    data class RedditChild(
        val data: RedditPost? = null,
    )

    data class RedditPost(
        val title: String?,
        val url: String?,
        val score: Int = 0,
        @JsonProperty("num_comments") val numComments: Int = 0,
        val subreddit: String?,
        val permalink: String?,
        val author: String?,
    )

    fun top5(): List<RedditPost> = data?.children
        ?.mapNotNull { it.data }
        ?.sortedByDescending { it.score }
        ?.take(5)
        ?: emptyList()
}
