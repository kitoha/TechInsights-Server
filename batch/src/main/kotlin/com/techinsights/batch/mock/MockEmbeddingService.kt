package com.techinsights.batch.mock

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
}
