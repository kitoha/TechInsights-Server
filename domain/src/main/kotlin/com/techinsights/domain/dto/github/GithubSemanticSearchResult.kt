package com.techinsights.domain.dto.github

data class GithubSemanticSearchResult(
    val fullName: String,
    val repoName: String,
    val description: String?,
    val readmeSummary: String?,
    val primaryLanguage: String?,
    val starCount: Long,
    val ownerName: String,
    val ownerAvatarUrl: String?,
    val topics: List<String>,
    val htmlUrl: String,
    val similarityScore: Double,
    val rank: Int,
)
