package com.techinsights.batch.github.embedding.config.props

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "batch.github-embedding")
data class GithubEmbeddingBatchProperties(
    val jobName: String = "githubReadmeEmbeddingJob",
    val stepName: String = "githubReadmeEmbeddingStep",
    /** 한 청크당 처리할 레포 수 */
    val chunkSize: Int = 10,
    /** DB에서 한 번에 읽어올 레포 수 */
    val pageSize: Int = 100,
    /** 1회 배치 실행당 처리할 최대 레포 수 */
    val maxItemsPerRun: Int = 1000,
)
