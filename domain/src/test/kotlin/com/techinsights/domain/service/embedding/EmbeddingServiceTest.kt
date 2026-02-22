package com.techinsights.domain.service.embedding

import com.techinsights.domain.dto.embedding.EmbeddingRequest
import com.techinsights.domain.enums.GeminiModelType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

class EmbeddingServiceTest : FunSpec({

    val embeddingService = mockk<EmbeddingService>()

    beforeTest {
        clearAllMocks()
    }

    context("generateQuestionEmbedding") {

        test("질문 텍스트로 임베딩 벡터를 반환한다") {
            val question = "토스에서 MSA 전환을 어떻게 했어?"
            val expectedVector = List(3072) { it.toFloat() / 3072f }

            every { embeddingService.generateQuestionEmbedding(question) } returns expectedVector

            val result = embeddingService.generateQuestionEmbedding(question)

            result shouldHaveSize 3072
            result shouldBe expectedVector
        }

        test("빈 질문도 처리할 수 있다") {
            val question = " "
            val expectedVector = List(3072) { 0.0f }

            every { embeddingService.generateQuestionEmbedding(question) } returns expectedVector

            val result = embeddingService.generateQuestionEmbedding(question)

            result shouldHaveSize 3072
        }

        test("generateQuestionEmbedding은 내부적으로 GEMINI_EMBEDDING 모델을 사용한다") {
            val question = "Kubernetes 배포 전략은?"
            val expectedVector = listOf(0.1f, 0.2f, 0.3f)
            val requestSlot = slot<EmbeddingRequest>()

            every {
                embeddingService.generateEmbedding(capture(requestSlot), GeminiModelType.GEMINI_EMBEDDING)
            } returns expectedVector

            every { embeddingService.generateQuestionEmbedding(question) } answers {
                embeddingService.generateEmbedding(
                    EmbeddingRequest(content = question, categories = emptyList(), companyName = ""),
                    GeminiModelType.GEMINI_EMBEDDING
                )
            }

            val result = embeddingService.generateQuestionEmbedding(question)

            result shouldBe expectedVector
            verify(exactly = 1) {
                embeddingService.generateEmbedding(any(), GeminiModelType.GEMINI_EMBEDDING)
            }
            requestSlot.captured.content shouldBe question
            requestSlot.captured.categories shouldBe emptyList()
            requestSlot.captured.companyName shouldBe ""
        }
    }
})
