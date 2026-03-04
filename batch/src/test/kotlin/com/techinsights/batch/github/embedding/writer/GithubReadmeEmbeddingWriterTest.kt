package com.techinsights.batch.github.embedding.writer

import com.techinsights.batch.github.embedding.dto.GithubEmbeddingResultDto
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.springframework.batch.item.Chunk
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

class GithubReadmeEmbeddingWriterTest : FunSpec({

    val jdbcTemplate = mockk<NamedParameterJdbcTemplate>(relaxed = true)
    val writer = GithubReadmeEmbeddingWriter(jdbcTemplate)

    beforeTest { clearAllMocks() }

    test("청크의 모든 항목을 batchUpdate로 DB에 업데이트한다") {
        val item1 = GithubEmbeddingResultDto(id = 1L, fullName = "owner/repo1", embeddingVector = floatArrayOf(0.1f, 0.2f))
        val item2 = GithubEmbeddingResultDto(id = 2L, fullName = "owner/repo2", embeddingVector = floatArrayOf(0.3f, 0.4f))

        writer.write(Chunk(listOf(item1, item2)))

        val capturedParams = mutableListOf<MapSqlParameterSource>()
        verify {
            jdbcTemplate.batchUpdate(
                any<String>(),
                withArg<Array<MapSqlParameterSource>> { params ->
                    capturedParams.addAll(params.toList())
                }
            )
        }
        capturedParams.size shouldBe 2
        capturedParams[0].getValue("id") shouldBe 1L
        capturedParams[1].getValue("id") shouldBe 2L
    }

    test("벡터를 pgvector 형식 문자열로 변환한다") {
        val item = GithubEmbeddingResultDto(id = 1L, fullName = "owner/repo", embeddingVector = floatArrayOf(0.1f, 0.2f, 0.3f))

        writer.write(Chunk(listOf(item)))

        val capturedParams = mutableListOf<MapSqlParameterSource>()
        verify {
            jdbcTemplate.batchUpdate(
                any<String>(),
                withArg<Array<MapSqlParameterSource>> { params ->
                    capturedParams.addAll(params.toList())
                }
            )
        }
        capturedParams[0].getValue("vector") shouldBe "[0.1,0.2,0.3]"
    }

    test("빈 청크는 batchUpdate를 호출하지 않는다") {
        writer.write(Chunk(emptyList()))

        verify(exactly = 0) { jdbcTemplate.batchUpdate(any<String>(), any<Array<MapSqlParameterSource>>()) }
    }
})
