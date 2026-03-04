package com.techinsights.domain.repository.github

interface GithubRepositoryWithDistance {
    val fullName: String
    val repoName: String
    val description: String?
    val readmeSummary: String?
    val primaryLanguage: String?
    val starCount: Long
    val ownerName: String
    val ownerAvatarUrl: String?
    val topics: String?
    val htmlUrl: String
    val distance: Double
}
