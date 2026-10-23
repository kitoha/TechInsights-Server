package com.techinsights.domain.infrastructure.gemini.embedding

import com.google.genai.Client
import com.google.genai.Models
import com.google.genai.types.ContentEmbedding
import com.google.genai.types.EmbedContentConfig
import com.google.genai.types.EmbedContentResponse
import com.techinsights.domain.config.cache.CacheConfig
import com.techinsights.domain.config.resilience.CircuitBreakerProperties
import com.techinsights.domain.config.resilience.RateLimiterProperties
import com.techinsights.domain.config.resilience.ResilienceConfig
import io.github.resilience4j.ratelimiter.RequestNotPermitted
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.Optional

class EmbeddingServiceImplTest : FunSpec({

    val vector = listOf(0.1f, 0.2f, 0.3f)

    fun embeddingResponse(): EmbedContentResponse {
        val embedding = mockk<ContentEmbedding>()
        every { embedding.values() } returns Optional.of(vector)
        val response = mockk<EmbedContentResponse>()
        every { response.embeddings() } returns Optional.of(listOf(embedding))
        return response
    }

    fun clientWith(models: Models): Client {
        val client = mockk<Client>()
        Client::class.java.getField("models").apply { isAccessible = true }.set(client, models)
        return client
    }

    fun service(models: Models, rateLimiterProperties: RateLimiterProperties = RateLimiterProperties()): EmbeddingServiceImpl {
        val registry = ResilienceConfig(rateLimiterProperties, CircuitBreakerProperties()).rateLimiterRegistry()
        return EmbeddingServiceImpl(clientWith(models), registry, CacheConfig().cacheManager())
    }

    test("같은 질문은 정규화 후 캐시되어 Gemini를 한 번만 호출한다") {
        val models = mockk<Models>()
        every { models.embedContent(any<String>(), any<String>(), any<EmbedContentConfig>()) } returns embeddingResponse()
        val embeddingService = service(models)

        embeddingService.generateQuestionEmbedding("Kotlin  Coroutines") shouldBe vector
        embeddingService.generateQuestionEmbedding("  kotlin coroutines ") shouldBe vector

        verify(exactly = 1) { models.embedContent(any<String>(), any<String>(), any<EmbedContentConfig>()) }
    }

    test("다른 질문은 각각 Gemini를 호출한다") {
        val models = mockk<Models>()
        every { models.embedContent(any<String>(), any<String>(), any<EmbedContentConfig>()) } returns embeddingResponse()
        val embeddingService = service(models)

        embeddingService.generateQuestionEmbedding("kotlin")
        embeddingService.generateQuestionEmbedding("spring")

        verify(exactly = 2) { models.embedContent(any<String>(), any<String>(), any<EmbedContentConfig>()) }
    }

    test("요청 경로 limiter가 소진되면 대기 없이 RequestNotPermitted를 던진다") {
        val models = mockk<Models>()
        every { models.embedContent(any<String>(), any<String>(), any<EmbedContentConfig>()) } returns embeddingResponse()
        val properties = RateLimiterProperties(
            geminiQueryEmbedding = RateLimiterProperties.LimiterConfig(
                limitForPeriod = 1,
                refreshPeriodSeconds = 3600,
                timeoutSeconds = 0
            )
        )
        val embeddingService = service(models, properties)

        embeddingService.generateQuestionEmbedding("first")
        val startedAt = System.nanoTime()
        shouldThrow<RequestNotPermitted> { embeddingService.generateQuestionEmbedding("second") }

        ((System.nanoTime() - startedAt) / 1_000_000 < 1_000) shouldBe true
        verify(exactly = 1) { models.embedContent(any<String>(), any<String>(), any<EmbedContentConfig>()) }
    }

    test("limiter 거절은 캐시에 남지 않아 이후 재시도가 가능하다") {
        val models = mockk<Models>()
        every { models.embedContent(any<String>(), any<String>(), any<EmbedContentConfig>()) } returns embeddingResponse()
        val properties = RateLimiterProperties(
            geminiQueryEmbedding = RateLimiterProperties.LimiterConfig(
                limitForPeriod = 1,
                refreshPeriodSeconds = 1,
                timeoutSeconds = 0
            )
        )
        val embeddingService = service(models, properties)

        embeddingService.generateQuestionEmbedding("first")
        shouldThrow<RequestNotPermitted> { embeddingService.generateQuestionEmbedding("second") }
        Thread.sleep(1_100)

        embeddingService.generateQuestionEmbedding("second") shouldBe vector
    }
})
