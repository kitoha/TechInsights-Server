package com.techinsights.batch.github.community.analyze.writer

import com.fasterxml.jackson.databind.ObjectMapper
import com.techinsights.domain.dto.community.CommunityAnalysisInput
import com.techinsights.domain.dto.community.CommunityPost
import com.techinsights.domain.service.gemini.CommunityAnalyzer
import com.techinsights.domain.dto.community.CommunityAnalysisResult
import kotlinx.coroutines.flow.toCollection
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
class CommunityAnalyzeWriter(
    private val analyzer: CommunityAnalyzer,
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    private val objectMapper: ObjectMapper,
) : ItemWriter<CommunityAnalysisInput> {

    override fun write(chunk: Chunk<out CommunityAnalysisInput>) {
        if (chunk.isEmpty) return

        val items = chunk.items.toList()
        val inputMap = items.associateBy { it.repoFullName }

        log.info("[CommunityAnalyzeWriter] Sending ${items.size} repos to Gemini for analysis")

        val results = try {
            runBlocking { analyzer.analyze(items).toCollection(mutableListOf<CommunityAnalysisResult>()) }
        } catch (e: Exception) {
            log.error("[CommunityAnalyzeWriter] Analyzer threw exception: ${e.message}, marking all as FAILED")
            val failedParams = items.map { input ->
                MapSqlParameterSource().addValue("id", input.repoFullName)
            }.toTypedArray()
            jdbcTemplate.batchUpdate(MARK_FAILED_SQL, failedParams)
            return
        }

        val successParams = results
            .filter { it.success && it.postClassifications.isNotEmpty() }
            .mapNotNull { result ->
                val input = inputMap[result.id] ?: return@mapNotNull null
                val mentionCount = input.hnPosts.size + input.redditPosts.size

                MapSqlParameterSource()
                    .addValue("id", result.id)
                    .addValue("insights", objectMapper.writeValueAsString(result.insights ?: emptyList<Any>()))
                    .addValue("mentionCount", mentionCount)
            }
            .toTypedArray()

        if (successParams.isNotEmpty()) {
            jdbcTemplate.batchUpdate(UPDATE_SUCCESS_SQL, successParams)
            log.info("[CommunityAnalyzeWriter] Updated ${successParams.size}/${items.size} community analyses")
        }

        val failedParams = results
            .filter { !it.success || it.postClassifications.isEmpty() }
            .map { result ->
                MapSqlParameterSource().addValue("id", result.id)
            }
            .toTypedArray()

        if (failedParams.isNotEmpty()) {
            jdbcTemplate.batchUpdate(MARK_FAILED_SQL, failedParams)
            log.warn("[CommunityAnalyzeWriter] ${failedParams.size}/${items.size} community analyses failed")
        }
    }

    private fun buildHighlights(hnPosts: List<CommunityPost>, redditPosts: List<CommunityPost>): List<CommunityPost> {
        return buildList {
            hnPosts.firstOrNull()?.let { add(it) }
            redditPosts.firstOrNull()?.let { add(it) }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(CommunityAnalyzeWriter::class.java)

        private const val UPDATE_SUCCESS_SQL = """
            UPDATE github_repositories
            SET community_sentiment      = :sentiment::jsonb,
                community_insights       = :insights::jsonb,
                community_highlights     = :highlights::jsonb,
                community_mention_count  = :mentionCount,
                community_fetched_at     = NOW(),
                community_status         = 'COMPLETED',
                community_error_type     = NULL,
                community_update_count   = community_update_count + 1
            WHERE full_name = :id
        """

        private const val MARK_FAILED_SQL = """
            UPDATE github_repositories
            SET community_fetched_at = NOW(),
                community_status     = 'FAILED',
                community_error_type = NULL
            WHERE full_name = :id
        """
    }
}
