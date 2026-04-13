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
            .filter { it.success }
            .mapNotNull { result ->
                val input = inputMap[result.id] ?: return@mapNotNull null
                val mentionCount = input.hnPosts.size + input.redditPosts.size
                val highlights = buildHighlights(input.hnPosts, input.redditPosts)

                MapSqlParameterSource()
                    .addValue("id", result.id)
                    .addValue("sentiment", objectMapper.writeValueAsString(result.sentiment))
                    .addValue("insights", objectMapper.writeValueAsString(result.insights ?: emptyList<Any>()))
                    .addValue("highlights", objectMapper.writeValueAsString(highlights))
                    .addValue("mentionCount", mentionCount)
            }
            .toTypedArray()

        if (successParams.isNotEmpty()) {
            jdbcTemplate.batchUpdate(UPDATE_SUCCESS_SQL, successParams)
            log.info("[CommunityAnalyzeWriter] Updated ${successParams.size}/${items.size} community analyses")
        }

        val failedParams = results
            .filter { !it.success }
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
            INSERT INTO github_repository_community (repo_id, community_sentiment, community_mention_count, community_highlights, community_insights, community_fetched_at, community_status, community_error_type, community_update_count, created_at, updated_at)
            SELECT id, :sentiment::jsonb, :mentionCount, :highlights::jsonb, :insights::jsonb, NOW(), 'COMPLETED', NULL, COALESCE((SELECT community_update_count FROM github_repository_community WHERE repo_id = gr.id), 0) + 1, NOW(), NOW()
            FROM github_repositories gr
            WHERE gr.full_name = :id
            ON CONFLICT (repo_id) DO UPDATE
                SET community_sentiment      = EXCLUDED.community_sentiment,
                    community_mention_count  = EXCLUDED.community_mention_count,
                    community_highlights     = EXCLUDED.community_highlights,
                    community_insights       = EXCLUDED.community_insights,
                    community_fetched_at     = EXCLUDED.community_fetched_at,
                    community_status         = 'COMPLETED',
                    community_error_type     = NULL,
                    community_update_count   = github_repository_community.community_update_count + 1,
                    updated_at               = NOW()
        """

        private const val MARK_FAILED_SQL = """
            INSERT INTO github_repository_community (repo_id, community_fetched_at, community_status, created_at, updated_at)
            SELECT id, NOW(), 'FAILED', NOW(), NOW()
            FROM github_repositories
            WHERE full_name = :id
            ON CONFLICT (repo_id) DO UPDATE
                SET community_fetched_at = NOW(),
                    community_status     = 'FAILED',
                    updated_at           = NOW()
        """
    }
}
