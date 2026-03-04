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

        private val UPSERT_SQL = """
            INSERT INTO github_repositories (
                id, repo_name, full_name, description, html_url,
                star_count, fork_count, primary_language, owner_name, owner_avatar_url,
                topics, pushed_at, fetched_at, weekly_star_delta,
                created_at, updated_at
            ) VALUES (
                :id, :repoName, :fullName, :description, :htmlUrl,
                :starCount, :forkCount, :primaryLanguage, :ownerName, :ownerAvatarUrl,
                :topics, :pushedAt, :fetchedAt, 0,
                NOW(), NOW()
            )
            ON CONFLICT (full_name) DO UPDATE SET
                star_count     = EXCLUDED.star_count,
                fork_count     = EXCLUDED.fork_count,
                description    = EXCLUDED.description,
                primary_language = EXCLUDED.primary_language,
                owner_avatar_url = EXCLUDED.owner_avatar_url,
                topics         = EXCLUDED.topics,
                pushed_at      = EXCLUDED.pushed_at,
                fetched_at     = EXCLUDED.fetched_at,
                weekly_star_delta = CASE
                    WHEN github_repositories.star_count_prev_week_updated_at IS NULL
                        OR github_repositories.star_count_prev_week_updated_at < NOW() - INTERVAL '7 days'
                    THEN EXCLUDED.star_count
                        - COALESCE(github_repositories.star_count_prev_week, EXCLUDED.star_count)
                    ELSE github_repositories.weekly_star_delta
                END,
                star_count_prev_week = CASE
                    WHEN github_repositories.star_count_prev_week_updated_at IS NULL
                        OR github_repositories.star_count_prev_week_updated_at < NOW() - INTERVAL '7 days'
                    THEN github_repositories.star_count
                    ELSE github_repositories.star_count_prev_week
                END,
                star_count_prev_week_updated_at = CASE
                    WHEN github_repositories.star_count_prev_week_updated_at IS NULL
                        OR github_repositories.star_count_prev_week_updated_at < NOW() - INTERVAL '7 days'
                    THEN NOW()
                    ELSE github_repositories.star_count_prev_week_updated_at
                END,
                updated_at = NOW()
        """.trimIndent()
    }
}
