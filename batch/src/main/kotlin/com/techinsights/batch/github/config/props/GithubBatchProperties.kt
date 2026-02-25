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

    /**
     * 3개의 연도 구간으로 나눠 GitHub Search API 한계(최대 1000건/쿼리)를 우회.
     * LocalDate.now().year 기반으로 매 호출 시 동적으로 생성된다.
     */
    val queries: List<QueryConfig>
        get() {
            val year = LocalDate.now().year
            return listOf(
                QueryConfig("stars:>100 created:${year - 2}-01-01..${year - 2}-12-31"),
                QueryConfig("stars:>100 created:${year - 1}-01-01..${year - 1}-12-31"),
                QueryConfig("stars:>100 created:${year}-01-01..${year}-12-31"),
            )
        }
}
