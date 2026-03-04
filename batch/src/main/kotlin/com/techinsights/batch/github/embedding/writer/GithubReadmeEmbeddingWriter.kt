package com.techinsights.batch.github.embedding.writer

import com.techinsights.batch.github.embedding.dto.GithubEmbeddingRequestDto
import com.techinsights.domain.enums.GeminiModelType
import com.techinsights.domain.service.embedding.EmbeddingService
import org.slf4j.LoggerFactory
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
class GithubReadmeEmbeddingWriter(
    private val embeddingService: EmbeddingService,
    private val jdbcTemplate: NamedParameterJdbcTemplate,
) : ItemWriter<GithubEmbeddingRequestDto> {

    override fun write(chunk: Chunk<out GithubEmbeddingRequestDto>) {
        val items = chunk.items
        if (items.isEmpty()) return

        // 청크 내 모든 promptString을 1번의 API 호출로 임베딩 (batchEmbedContents)
        val vectors = embeddingService.batchEmbedTexts(
            items.map { it.promptString },
            GeminiModelType.GEMINI_EMBEDDING,
        )

        val params = items.zip(vectors)
            .filter { (_, vector) -> vector.isNotEmpty() }
            .map { (dto, vector) ->
                val vectorString = vector.joinToString(",", "[", "]")
                MapSqlParameterSource()
                    .addValue("id", dto.id)
                    .addValue("vector", vectorString)
            }.toTypedArray()

        if (params.isEmpty()) {
            log.warn("[EmbeddingWriter] All embeddings were empty for chunk of ${items.size} items")
            return
        }

        jdbcTemplate.batchUpdate(UPDATE_SQL, params)
        val skipped = items.size - params.size
        log.info("[EmbeddingWriter] Updated ${params.size} embedding vectors" +
            if (skipped > 0) " (skipped $skipped empty)" else "")
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
