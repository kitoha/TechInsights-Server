package com.techinsights.batch.github.readme.config.props

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "batch.github-readme-summary")
data class GithubReadmeBatchProperties(
    val jobName: String = "githubReadmeSummaryJob",
    val stepName: String = "githubReadmeSummarizeStep",
    /** Gemini 요청 1회 당 처리할 레포 수 (= chunk-size) */
    val chunkSize: Int = 150,
    /** DB에서 한 번에 읽어올 레포 수 */
    val pageSize: Int = 100,
)
