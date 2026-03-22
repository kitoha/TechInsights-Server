package com.techinsights.batch.github.readme.config.props

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "batch.github-readme-summary")
data class GithubReadmeBatchProperties(
    val jobName: String = "githubReadmeSummaryJob",
    val stepName: String = "githubReadmeSummarizeStep",
    val chunkSize: Int = 150,
    val pageSize: Int = 100,
    val maxItemsPerRun: Int = 3000,
    val retryEnabled: Boolean = true,
    val retryAfterDays: Long = 7L,
)
