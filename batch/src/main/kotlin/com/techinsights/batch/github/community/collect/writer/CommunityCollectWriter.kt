package com.techinsights.batch.github.community.collect.writer

import com.fasterxml.jackson.databind.ObjectMapper
import com.techinsights.batch.github.community.dto.CommunityBuzzInput
import org.slf4j.LoggerFactory
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
class CommunityCollectWriter(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    private val objectMapper: ObjectMapper,
) : ItemWriter<CommunityBuzzInput> {

    override fun write(chunk: Chunk<out CommunityBuzzInput>) {
        if (chunk.isEmpty) return

        val items = chunk.items.toList()
        log.info("[CommunityCollectWriter] Writing ${items.size} community collect results")

        val noMentionsParams = items
            .filter { it.totalMentions == 0 }
            .map { input ->
                MapSqlParameterSource()
                    .addValue("fullName", input.repoFullName)
            }
            .toTypedArray()

        if (noMentionsParams.isNotEmpty()) {
            jdbcTemplate.batchUpdate(NO_MENTIONS_SQL, noMentionsParams)
            log.info("[CommunityCollectWriter] Marked ${noMentionsParams.size} repos as NO_MENTIONS")
        }

        val pendingParams = items
            .filter { it.totalMentions > 0 }
            .map { input ->
                val highlights = buildHighlightsJson(input)
                MapSqlParameterSource()
                    .addValue("fullName", input.repoFullName)
                    .addValue("highlights", highlights)
                    .addValue("rawMentionCount", input.totalMentions)
            }
            .toTypedArray()

        if (pendingParams.isNotEmpty()) {
            jdbcTemplate.batchUpdate(PENDING_SQL, pendingParams)
            log.info("[CommunityCollectWriter] Marked ${pendingParams.size} repos as PENDING with highlights")
        }
    }

    private fun buildHighlightsJson(input: CommunityBuzzInput): String {
        val data = mapOf(
            "hnPosts" to input.hnPosts,
            "redditPosts" to input.redditPosts,
        )
        return objectMapper.writeValueAsString(data)
    }

    companion object {
        private val log = LoggerFactory.getLogger(CommunityCollectWriter::class.java)

        private const val NO_MENTIONS_SQL = """
            UPDATE github_repositories
            SET community_highlights        = NULL,
                community_raw_mention_count = 0,
                community_collected_at      = NOW(),
                community_status            = 'NO_MENTIONS'
            WHERE full_name = :fullName
        """

        private const val PENDING_SQL = """
            UPDATE github_repositories
            SET community_highlights        = :highlights::jsonb,
                community_raw_mention_count = :rawMentionCount,
                community_collected_at      = NOW(),
                community_status            = 'PENDING'
            WHERE full_name = :fullName
        """
    }
}
