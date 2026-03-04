package com.techinsights.batch.github.embedding.writer

import com.techinsights.batch.github.embedding.dto.GithubEmbeddingRequestDto
import com.techinsights.domain.enums.GeminiModelType
import com.techinsights.domain.service.embedding.EmbeddingService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.springframework.batch.item.Chunk
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

class GithubReadmeEmbeddingWriterTest : FunSpec({

    val embeddingService = mockk<EmbeddingService>()
    val jdbcTemplate = mockk<NamedParameterJdbcTemplate>(relaxed = true)
    val writer = GithubReadmeEmbeddingWriter(embeddingService, jdbcTemplate)

    beforeTest { clearAllMocks() }

    test("청크의 promptString을 모아 batchEmbedTexts를 1회만 호출한다") {
        val items = listOf(
            GithubEmbeddingRequestDto(id = 1L, fullName = "owner/repo1", promptString = "prompt1"),
            GithubEmbeddingRequestDto(id = 2L, fullName = "owner/repo2", promptString = "prompt2"),
        )
        every { embeddingService.batchEmbedTexts(any(), any()) } returns listOf(
            listOf(0.1f, 0.2f),
            listOf(0.3f, 0.4f),
        )

        writer.write(Chunk(items))

        verify(exactly = 1) {
            embeddingService.batchEmbedTexts(
                listOf("prompt1", "prompt2"),
                GeminiModelType.GEMINI_EMBEDDING,
            )
        }
    }

    test("성공한 임베딩을 batchUpdate로 DB에 업데이트한다") {
        val items = listOf(
            GithubEmbeddingRequestDto(id = 1L, fullName = "owner/repo1", promptString = "prompt1"),
            GithubEmbeddingRequestDto(id = 2L, fullName = "owner/repo2", promptString = "prompt2"),
        )
        every { embeddingService.batchEmbedTexts(any(), any()) } returns listOf(
            listOf(0.1f, 0.2f),
            listOf(0.3f, 0.4f),
        )

        writer.write(Chunk(items))

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
        val item = GithubEmbeddingRequestDto(id = 1L, fullName = "owner/repo", promptString = "prompt")
        every { embeddingService.batchEmbedTexts(any(), any()) } returns listOf(listOf(0.1f, 0.2f, 0.3f))

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

    test("임베딩이 빈 벡터면 해당 항목은 DB 업데이트에서 제외한다") {
        val items = listOf(
            GithubEmbeddingRequestDto(id = 1L, fullName = "owner/repo1", promptString = "prompt1"),
            GithubEmbeddingRequestDto(id = 2L, fullName = "owner/repo2", promptString = "prompt2"),
        )
        every { embeddingService.batchEmbedTexts(any(), any()) } returns listOf(
            listOf(0.1f, 0.2f),
            emptyList(),
        )

        writer.write(Chunk(items))

        val capturedParams = mutableListOf<MapSqlParameterSource>()
        verify {
            jdbcTemplate.batchUpdate(
                any<String>(),
                withArg<Array<MapSqlParameterSource>> { params ->
                    capturedParams.addAll(params.toList())
                }
            )
        }
        capturedParams.size shouldBe 1
        capturedParams[0].getValue("id") shouldBe 1L
    }

    test("빈 청크는 API를 호출하지 않고 DB도 업데이트하지 않는다") {
        writer.write(Chunk(emptyList()))

        verify(exactly = 0) { embeddingService.batchEmbedTexts(any(), any()) }
        verify(exactly = 0) { jdbcTemplate.batchUpdate(any<String>(), any<Array<MapSqlParameterSource>>()) }
    }

    test("모든 임베딩이 빈 벡터면 batchUpdate를 호출하지 않는다") {
        val items = listOf(
            GithubEmbeddingRequestDto(id = 1L, fullName = "owner/repo1", promptString = "prompt1"),
            GithubEmbeddingRequestDto(id = 2L, fullName = "owner/repo2", promptString = "prompt2"),
        )
        every { embeddingService.batchEmbedTexts(any(), any()) } returns listOf(emptyList(), emptyList())

        writer.write(Chunk(items))

        verify(exactly = 0) { jdbcTemplate.batchUpdate(any<String>(), any<Array<MapSqlParameterSource>>()) }
    }
})
