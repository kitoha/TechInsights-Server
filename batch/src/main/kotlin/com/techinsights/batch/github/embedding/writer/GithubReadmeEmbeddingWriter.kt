package com.techinsights.batch.github.embedding.writer

import com.techinsights.batch.github.embedding.dto.GithubEmbeddingResultDto
import org.slf4j.LoggerFactory
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
class GithubReadmeEmbeddingWriter(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
) : ItemWriter<GithubEmbeddingResultDto> {

    override fun write(chunk: Chunk<out GithubEmbeddingResultDto>) {
        val items = chunk.items
        if (items.isEmpty()) return

        val params = items.map { dto ->
            val vectorString = dto.embeddingVector.joinToString(",", "[", "]")
            MapSqlParameterSource()
                .addValue("id", dto.id)
                .addValue("vector", vectorString)
        }.toTypedArray()

        jdbcTemplate.batchUpdate(UPDATE_SQL, params)
        log.info("[EmbeddingWriter] Updated ${items.size} embedding vectors")
    }

    companion object {
        private val log = LoggerFactory.getLogger(GithubReadmeEmbeddingWriter::class.java)

        private const val UPDATE_SQL = """
            UPDATE github_repositories
            SET readme_embedding_vector = CAST(:vector AS vector),
                readme_embedded_at      = NOW()
            WHERE id = :id
        """
    }
}
