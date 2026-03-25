package com.techinsights.batch.github.writer

import com.techinsights.batch.github.dto.GithubRepoUpsertData
import org.slf4j.LoggerFactory
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
class GithubRepoWriter(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
) : ItemWriter<GithubRepoUpsertData> {

    override fun write(chunk: Chunk<out GithubRepoUpsertData>) {
        if (chunk.isEmpty) return

        val params = chunk.items.map { it.toSqlParams() }.toTypedArray()
        jdbcTemplate.batchUpdate(UPSERT_SQL, params)

        log.info("[GithubRepoWriter] Upserted ${chunk.size()} repositories")
    }

    private fun GithubRepoUpsertData.toSqlParams(): MapSqlParameterSource =
        MapSqlParameterSource()
            .addValue("id", id)
            .addValue("repoName", repoName)
            .addValue("fullName", fullName)
            .addValue("description", description)
            .addValue("htmlUrl", htmlUrl)
            .addValue("starCount", starCount)
            .addValue("forkCount", forkCount)
            .addValue("primaryLanguage", primaryLanguage)
            .addValue("ownerName", ownerName)
            .addValue("ownerAvatarUrl", ownerAvatarUrl)
            .addValue("topics", topics)
            .addValue("pushedAt", pushedAt)
            .addValue("fetchedAt", fetchedAt)

    companion object {
        private val log = LoggerFactory.getLogger(GithubRepoWriter::class.java)

        private fun starDeltaBlock(
            deltaCol: String,
            prevCol: String,
            updatedAtCol: String,
            thresholdSql: String,
        ): String = """
            $deltaCol = CASE
                WHEN github_repositories.$updatedAtCol IS NULL
                    OR github_repositories.$updatedAtCol < $thresholdSql
                THEN EXCLUDED.star_count
                    - COALESCE(github_repositories.$prevCol, EXCLUDED.star_count)
                ELSE github_repositories.$deltaCol
            END,
            $prevCol = CASE
                WHEN github_repositories.$updatedAtCol IS NULL
                    OR github_repositories.$updatedAtCol < $thresholdSql
                THEN EXCLUDED.star_count
                ELSE github_repositories.$prevCol
            END,
            $updatedAtCol = CASE
                WHEN github_repositories.$updatedAtCol IS NULL
                    OR github_repositories.$updatedAtCol < $thresholdSql
                THEN NOW()
                ELSE github_repositories.$updatedAtCol
            END,
        """.trimIndent()

        private val UPSERT_SQL = """
            INSERT INTO github_repositories (
                id, repo_name, full_name, description, html_url,
                star_count, fork_count, primary_language, owner_name, owner_avatar_url,
                topics, pushed_at, fetched_at, weekly_star_delta, daily_star_delta,
                created_at, updated_at
            ) VALUES (
                :id, :repoName, :fullName, :description, :htmlUrl,
                :starCount, :forkCount, :primaryLanguage, :ownerName, :ownerAvatarUrl,
                :topics, :pushedAt, :fetchedAt, 0, 0,
                NOW(), NOW()
            )
            ON CONFLICT (full_name) DO UPDATE SET
                star_count       = EXCLUDED.star_count,
                fork_count       = EXCLUDED.fork_count,
                description      = EXCLUDED.description,
                primary_language = EXCLUDED.primary_language,
                owner_avatar_url = EXCLUDED.owner_avatar_url,
                topics           = EXCLUDED.topics,
                pushed_at        = EXCLUDED.pushed_at,
                fetched_at       = EXCLUDED.fetched_at,
                ${starDeltaBlock("weekly_star_delta", "star_count_prev_week", "star_count_prev_week_updated_at", "NOW() - INTERVAL '7 days'")}
                ${starDeltaBlock("daily_star_delta", "star_count_prev_day", "star_count_prev_day_updated_at", "DATE_TRUNC('day', NOW())")}
                updated_at = NOW()
            WHERE github_repositories.deleted_at IS NULL
        """.trimIndent()
    }
}
