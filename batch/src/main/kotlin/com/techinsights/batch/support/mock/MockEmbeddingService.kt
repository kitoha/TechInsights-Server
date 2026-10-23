package com.techinsights.batch.support.mock

import com.techinsights.domain.dto.embedding.EmbeddingRequest
import com.techinsights.domain.service.embedding.EmbeddingService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service


@Service
@Primary
@Profile("perf-test")
class MockEmbeddingService(
    // 실제 Gemini 임베딩 API 레이턴시 시뮬레이션. 성능 테스트 프로파일 기본값을 유지하고, 단위 테스트에서는 0으로 설정한다.
    @Value("\${mock.embedding.api-response-time-ms:200}") private val apiResponseTimeMs: Long = 200L,
    @Value("\${mock.embedding.per-item-response-time-ms:10}") private val perItemResponseTimeMs: Long = 10L
) : EmbeddingService {

    private val log = LoggerFactory.getLogger(MockEmbeddingService::class.java)

    companion object {
        private const val EMBEDDING_DIMENSION = 768
    }

    override fun generateEmbedding(request: EmbeddingRequest): List<Float> {
        val startTime = System.currentTimeMillis()

        Thread.sleep(apiResponseTimeMs)

        val embedding = List(EMBEDDING_DIMENSION) { index ->
            (index.toFloat() / EMBEDDING_DIMENSION)
        }

        val elapsedTime = System.currentTimeMillis() - startTime
        log.debug("Mock embedding generated in {}ms (dimension: {})", elapsedTime, EMBEDDING_DIMENSION)

        return embedding
    }

    override fun generateQuestionEmbedding(question: String): List<Float> {
        Thread.sleep(apiResponseTimeMs)
        return List(EMBEDDING_DIMENSION) { index -> (index.toFloat() / EMBEDDING_DIMENSION) }
    }

    override fun generateEmbeddingBatch(
        requests: List<EmbeddingRequest>
    ): List<EmbeddingService.EmbeddingResult> {
        val startTime = System.currentTimeMillis()

        val batchResponseTime = apiResponseTimeMs + (requests.size * perItemResponseTimeMs)
        Thread.sleep(batchResponseTime)

        val results = requests.map { request ->
            val embedding = List(EMBEDDING_DIMENSION) { index ->
                (index.toFloat() / EMBEDDING_DIMENSION)
            }
            EmbeddingService.EmbeddingResult(
                request = request,
                vector = embedding,
                success = true
            )
        }

        val elapsedTime = System.currentTimeMillis() - startTime
        log.debug(
            "Mock batch embedding generated for {} items in {}ms (avg: {}ms per item, dimension: {})",
            requests.size, elapsedTime, elapsedTime / requests.size, EMBEDDING_DIMENSION
        )

        return results
    }

    override fun batchEmbedTexts(
        promptStrings: List<String>
    ): List<List<Float>> {
        Thread.sleep(apiResponseTimeMs + (promptStrings.size * perItemResponseTimeMs))
        return promptStrings.map {
            List(EMBEDDING_DIMENSION) { index -> (index.toFloat() / EMBEDDING_DIMENSION) }
        }
    }
}
