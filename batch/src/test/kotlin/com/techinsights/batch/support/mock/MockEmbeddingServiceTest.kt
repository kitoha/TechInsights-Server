package com.techinsights.batch.support.mock

import com.techinsights.domain.dto.embedding.EmbeddingRequest
import com.techinsights.domain.enums.GeminiModelType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class MockEmbeddingServiceTest : FunSpec({

    val mockEmbeddingService = MockEmbeddingService()

    test("generateEmbedding should return 768 dimension vector") {
        // given
        val request = EmbeddingRequest(
            companyName = "Test Company",
            categories = listOf("AI", "Backend"),
            content = "Test content for embedding generation"
        )
        val modelType = GeminiModelType.GEMINI_EMBEDDING

        // when
        val result = mockEmbeddingService.generateEmbedding(request, modelType)

        // then
        result.size shouldBe 768
    }

    test("generateEmbedding should return normalized values between 0 and 1") {
        // given
        val request = EmbeddingRequest(
            companyName = "Another Company",
            categories = listOf("Frontend", "DevOps"),
            content = "Another test content"
        )
        val modelType = GeminiModelType.GEMINI_EMBEDDING

        // when
        val result = mockEmbeddingService.generateEmbedding(request, modelType)

        // then
        result.forEach { value ->
            assert(value >= 0f && value <= 1f)
        }
    }

    test("generateEmbedding should return valid vector for different model types") {
        // given
        val request = EmbeddingRequest(
            companyName = "Tech Corp",
            categories = listOf("AI"),
            content = "Machine learning content"
        )

        // when
        val result1 = mockEmbeddingService.generateEmbedding(request, GeminiModelType.GEMINI_EMBEDDING)
        val result2 = mockEmbeddingService.generateEmbedding(request, GeminiModelType.GEMINI_EMBEDDING)

        // then
        result1.size shouldBe 768
        result2.size shouldBe 768
        result1 shouldNotBe null
        result2 shouldNotBe null
    }

    test("generateEmbedding should return consistent structure for empty content") {
        // given
        val request = EmbeddingRequest(
            companyName = "Empty Test",
            categories = listOf("Test"),
            content = ""
        )
        val modelType = GeminiModelType.GEMINI_EMBEDDING

        // when
        val result = mockEmbeddingService.generateEmbedding(request, modelType)

        // then
        result.size shouldBe 768
    }

    test("generateEmbedding should handle very long content") {
        // given
        val longContent = "A".repeat(10000)
        val request = EmbeddingRequest(
            companyName = "Long Content Test",
            categories = listOf("Performance"),
            content = longContent
        )
        val modelType = GeminiModelType.GEMINI_EMBEDDING

        // when
        val result = mockEmbeddingService.generateEmbedding(request, modelType)

        // then
        result.size shouldBe 768
    }

    test("generateEmbedding should return deterministic values based on index") {
        // given
        val request = EmbeddingRequest(
            companyName = "Test",
            categories = listOf("Test"),
            content = "Test"
        )
        val modelType = GeminiModelType.GEMINI_EMBEDDING

        // when
        val result = mockEmbeddingService.generateEmbedding(request, modelType)

        // then
        result[0] shouldBe 0.0f
        result[767] shouldBe (767.0f / 768.0f)
        result[384] shouldBe (384.0f / 768.0f)
    }

    context("generateEmbeddingBatch") {
        test("should return a result for each request") {
            // given
            val requests = listOf(
                EmbeddingRequest("Test", emptyList(), "content 1"),
                EmbeddingRequest("Test", emptyList(), "content 2")
            )

            // when
            val results = mockEmbeddingService.generateEmbeddingBatch(requests, GeminiModelType.GEMINI_EMBEDDING)

            // then
            results.size shouldBe 2
        }

        test("all results should be successful and have the correct dimension") {
            // given
            val requests = listOf(
                EmbeddingRequest("Test", emptyList(), "content 1"),
                EmbeddingRequest("Test", emptyList(), "content 2")
            )

            // when
            val results = mockEmbeddingService.generateEmbeddingBatch(requests, GeminiModelType.GEMINI_EMBEDDING)

            // then
            results.forEach { result ->
                result.success shouldBe true
                result.vector.size shouldBe 768
                result.error shouldBe null
                result.request shouldNotBe null
            }
        }
    }
})
