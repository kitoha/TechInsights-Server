package com.techinsights.batch.github.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class GithubSearchResponse(
    @JsonProperty("total_count") val totalCount: Int,
    @JsonProperty("incomplete_results") val incompleteResults: Boolean = false,
    val items: List<Item>,
) {
    data class Item(
        val id: Long,
        val name: String,
        @JsonProperty("full_name") val fullName: String,
        val description: String?,
        @JsonProperty("html_url") val htmlUrl: String,
        @JsonProperty("stargazers_count") val stargazersCount: Long,
        @JsonProperty("forks_count") val forksCount: Long,
        val language: String?,
        val owner: Owner,
        val topics: List<String> = emptyList(),
        @JsonProperty("pushed_at") val pushedAt: String,
    )

    data class Owner(
        val login: String,
        @JsonProperty("avatar_url") val avatarUrl: String?,
    )
}
