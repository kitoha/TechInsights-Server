package com.techinsights.batch.github.readme.writer

import com.techinsights.domain.dto.gemini.ArticleInput
import com.techinsights.domain.enums.ErrorType
import com.techinsights.domain.service.gemini.GithubReadmeBatchSummarizer
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
class GithubReadmeBatchSummaryWriter(
    private val summarizer: GithubReadmeBatchSummarizer,
    private val jdbcTemplate: NamedParameterJdbcTemplate,
) : ItemWriter<ArticleInput> {

    override fun write(chunk: Chunk<out ArticleInput>) {
        if (chunk.isEmpty) return

        val items = chunk.items.toList()
        log.info("[ReadmeSummaryWriter] Sending ${items.size} READMEs to Gemini")

        val results = runBlocking { summarizer.summarize(items).toList() }

        val successParams = results
            .filter { it.success && it.summary != null }
            .map { result ->
                MapSqlParameterSource()
                    .addValue("summary", result.summary)
                    .addValue("fullName", result.id)
            }
            .toTypedArray()

        if (successParams.isNotEmpty()) {
            jdbcTemplate.batchUpdate(UPDATE_SQL, successParams)
            log.info("[ReadmeSummaryWriter] Updated ${successParams.size}/${items.size} readme summaries")
        }

        val failedResults = results.filter { !it.success || it.summary == null }
        val failedParams = failedResults
            .map { result ->
                MapSqlParameterSource()
                    .addValue("fullName", result.id)
                    .addValue("errorType", (result.errorType ?: ErrorType.UNKNOWN).name)
            }
            .toTypedArray()

        if (failedParams.isNotEmpty()) {
            jdbcTemplate.batchUpdate(MARK_FAILED_SQL, failedParams)
            val errorDist = failedResults.groupBy { it.errorType?.name ?: "UNKNOWN" }.mapValues { it.value.size }
            log.warn("[ReadmeSummaryWriter] ${failedParams.size}/${items.size} summaries failed. errorTypes=$errorDist")
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(GithubReadmeBatchSummaryWriter::class.java)

        private const val UPDATE_SQL = """
            INSERT INTO github_repository_readme (repo_id, readme_summary, readme_summarized_at, readme_summary_error_type, created_at, updated_at)
            SELECT id, :summary, NOW(), NULL, NOW(), NOW()
            FROM github_repositories
            WHERE full_name = :fullName
            ON CONFLICT (repo_id) DO UPDATE
                SET readme_summary            = EXCLUDED.readme_summary,
                    readme_summarized_at      = EXCLUDED.readme_summarized_at,
                    readme_summary_error_type = NULL,
                    updated_at                = NOW()
        """

        private const val MARK_FAILED_SQL = """
            INSERT INTO github_repository_readme (repo_id, readme_summarized_at, readme_summary_error_type, created_at, updated_at)
            SELECT id, NOW(), :errorType, NOW(), NOW()
            FROM github_repositories
            WHERE full_name = :fullName
            ON CONFLICT (repo_id) DO UPDATE
                SET readme_summarized_at      = EXCLUDED.readme_summarized_at,
                    readme_summary_error_type = EXCLUDED.readme_summary_error_type,
                    updated_at                = NOW()
        """
    }
}
