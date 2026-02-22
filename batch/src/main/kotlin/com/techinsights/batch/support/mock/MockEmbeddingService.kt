package com.techinsights.batch.support.mock

import com.techinsights.domain.dto.embedding.EmbeddingRequest
import com.techinsights.domain.enums.GeminiModelType
import com.techinsights.domain.service.embedding.EmbeddingService
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service


@Service
@Primary
@Profile("perf-test")
class MockEmbeddingService : EmbeddingService {

    private val log = LoggerFactory.getLogger(MockEmbeddingService::class.java)

    companion object {
        private const val API_RESPONSE_TIME_MS = 200L
        private const val EMBEDDING_DIMENSION = 768
    }

    override fun generateEmbedding(request: EmbeddingRequest, modelType: GeminiModelType): List<Float> {
        val startTime = System.currentTimeMillis()

        Thread.sleep(API_RESPONSE_TIME_MS)

        val embedding = List(EMBEDDING_DIMENSION) { index ->
            (index.toFloat() / EMBEDDING_DIMENSION)
        }

        val elapsedTime = System.currentTimeMillis() - startTime
        log.debug("Mock embedding generated in {}ms (dimension: {})", elapsedTime, EMBEDDING_DIMENSION)

        return embedding
    }

    override fun generateQuestionEmbedding(question: String): List<Float> {
        Thread.sleep(API_RESPONSE_TIME_MS)
        return List(EMBEDDING_DIMENSION) { index -> (index.toFloat() / EMBEDDING_DIMENSION) }
    }

    override fun generateEmbeddingBatch(
        requests: List<EmbeddingRequest>,
        modelType: GeminiModelType
    ): List<EmbeddingService.EmbeddingResult> {
        val startTime = System.currentTimeMillis()

        val batchResponseTime = API_RESPONSE_TIME_MS + (requests.size * 10)
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
}
