package com.techinsights.batch.github.config.props

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.LocalDate

@ConfigurationProperties(prefix = "batch.github-trending")
data class GithubBatchProperties(
    val jobName: String = "githubTrendingJob",
    val stepName: String = "githubTrendingStep",
    val chunkSize: Int = 50,
) {
    data class QueryConfig(val query: String)

    val queries: List<QueryConfig>
        get() {
            val year = LocalDate.now().year
            return listOf(
                QueryConfig("stars:>1000 created:${year - 2}-01-01..${year - 2}-12-31"),
                QueryConfig("stars:>1000 created:${year - 1}-01-01..${year - 1}-12-31"),
                QueryConfig("stars:>1000 created:${year}-01-01..${year}-12-31"),
            )
        }
}
