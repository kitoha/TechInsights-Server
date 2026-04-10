package com.techinsights.batch.github.community.analyze.config.props

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "batch.community-analyze")
data class CommunityAnalyzeBatchProperties(
    val jobName: String = "communityAnalyzeJob",
    val stepName: String = "communityAnalyzeStep",
    val chunkSize: Int = 10,
    val pageSize: Int = 50,
    val maxItemsPerRun: Int = 200,
)
