package com.techinsights.batch.github.community.collect.config.props

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "batch.community-collect")
data class CommunityCollectBatchProperties(
    val jobName: String = "communityCollectJob",
    val stepName: String = "communityCollectStep",
    val chunkSize: Int = 50,
    val pageSize: Int = 100,
    val maxItemsPerRun: Int = 2834,
    val normalRefreshDays: Long = 14L,
    val noMentionsRefreshDays: Long = 30L,
)
