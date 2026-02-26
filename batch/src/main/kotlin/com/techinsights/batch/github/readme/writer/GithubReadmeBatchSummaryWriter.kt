package com.techinsights.batch.github.readme.writer

import com.techinsights.domain.dto.gemini.ArticleInput
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

        val failedCount = results.count { !it.success || it.summary == null }
        if (failedCount > 0) {
            log.warn("[ReadmeSummaryWriter] $failedCount/${items.size} summaries failed (including success=true but summary=null) — will be retried in next run")
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(GithubReadmeBatchSummaryWriter::class.java)

        private const val UPDATE_SQL = """
            UPDATE github_repositories
            SET readme_summary = :summary,
                readme_summarized_at = NOW()
            WHERE full_name = :fullName
        """
    }
}
