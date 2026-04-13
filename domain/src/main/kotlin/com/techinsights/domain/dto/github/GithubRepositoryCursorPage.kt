package com.techinsights.domain.dto.github

data class GithubRepositoryCursorPage(
    val content: List<GithubRepositoryDto>,
    val size: Int,
    val hasNext: Boolean,
    val nextCursor: String?,
)
