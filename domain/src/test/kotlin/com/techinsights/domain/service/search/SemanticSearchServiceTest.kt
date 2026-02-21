package com.techinsights.domain.service.search

import com.techinsights.domain.config.search.SemanticSearchProperties
import com.techinsights.domain.dto.company.CompanyDto
import com.techinsights.domain.dto.embedding.PostEmbeddingDto
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.enums.Category
import com.techinsights.domain.repository.post.PostEmbeddingRepository
import com.techinsights.domain.repository.post.PostRepository
import com.techinsights.domain.service.embedding.EmbeddingService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime

class SemanticSearchServiceTest : FunSpec({

    val embeddingService = mockk<EmbeddingService>()
    val postEmbeddingRepository = mockk<PostEmbeddingRepository>()
    val postRepository = mockk<PostRepository>()
    val properties = SemanticSearchProperties(defaultSize = 10, maxSize = 20)

    val service = SemanticSearchServiceImpl(
        embeddingService = embeddingService,
        postEmbeddingRepository = postEmbeddingRepository,
        postRepository = postRepository,
        properties = properties
    )

    val sampleCompany = CompanyDto(
        id = "1",
        name = "Toss",
        blogUrl = "https://toss.tech",
        logoImageName = "toss.png",
        rssSupported = true,
        totalViewCount = 1000,
        postCount = 50
    )

    val samplePost = PostDto(
        id = "post-1",
        title = "토스 MSA 전환기",
        preview = "토스는 MSA 전환을 단계적으로 진행했다.",
        url = "https://toss.tech/msa",
        content = "## MSA 전환\n- gRPC 게이트웨이 도입\n- 도메인 분리",
        publishedAt = LocalDateTime.now(),
        thumbnail = null,
        company = sampleCompany,
        viewCount = 500,
        categories = setOf(Category.BackEnd),
        isSummary = true,
        isEmbedding = true
    )

    val sampleEmbedding = PostEmbeddingDto(
        postId = "post-1",
        companyName = "Toss",
        categories = "BackEnd",
        content = "토스는 MSA 전환을 단계적으로 진행했다.",
        embeddingVector = FloatArray(3072) { 0.1f }
    )

    val questionVector = List(3072) { 0.1f }

    beforeTest {
        clearAllMocks()
    }

    context("search") {

        test("질문과 관련된 게시글 목록을 유사도 순서로 반환한다") {
            every { embeddingService.generateQuestionEmbedding(any()) } returns questionVector
            every { postEmbeddingRepository.findSimilarPosts(any(), any(), any()) } returns listOf(sampleEmbedding)
            every { postRepository.findAllByIdIn(listOf("post-1")) } returns listOf(samplePost)

            val results = service.search(query = "토스 MSA 전환", size = 10, companyId = null)

            results shouldHaveSize 1
            results[0].post.title shouldBe "토스 MSA 전환기"
            results[0].rank shouldBe 1
        }

        test("유사도 점수는 0.0 초과 1.0 이하여야 한다") {
            every { embeddingService.generateQuestionEmbedding(any()) } returns questionVector
            every { postEmbeddingRepository.findSimilarPosts(any(), any(), any()) } returns listOf(sampleEmbedding)
            every { postRepository.findAllByIdIn(any()) } returns listOf(samplePost)

            val results = service.search(query = "MSA", size = 10, companyId = null)

            results[0].similarityScore shouldBeGreaterThan 0.0
            results[0].similarityScore shouldBeLessThanOrEqualTo 1.0
        }

        test("관련 게시글이 없으면 빈 목록을 반환한다") {
            every { embeddingService.generateQuestionEmbedding(any()) } returns questionVector
            every { postEmbeddingRepository.findSimilarPosts(any(), any(), any()) } returns emptyList()

            val results = service.search(query = "없는 주제", size = 10, companyId = null)

            results.shouldBeEmpty()
            verify(exactly = 0) { postRepository.findAllByIdIn(any()) }
        }

        test("size 파라미터가 findSimilarPosts의 limit으로 전달된다") {
            every { embeddingService.generateQuestionEmbedding(any()) } returns questionVector
            every { postEmbeddingRepository.findSimilarPosts(any(), emptyList(), 5L) } returns emptyList()

            service.search(query = "쿠버네티스", size = 5, companyId = null)

            verify(exactly = 1) { postEmbeddingRepository.findSimilarPosts(any(), emptyList(), 5L) }
        }

        test("companyId 필터가 있으면 해당 회사 게시글만 반환한다") {
            val otherCompany = sampleCompany.copy(id = "2", name = "Kakao")
            val otherPost = samplePost.copy(id = "post-2", company = otherCompany)
            val otherEmbedding = sampleEmbedding.copy(postId = "post-2", companyName = "Kakao")

            every { embeddingService.generateQuestionEmbedding(any()) } returns questionVector
            every {
                postEmbeddingRepository.findSimilarPosts(any(), any(), any())
            } returns listOf(sampleEmbedding, otherEmbedding)
            every {
                postRepository.findAllByIdIn(listOf("post-1", "post-2"))
            } returns listOf(samplePost, otherPost)

            val results = service.search(query = "MSA", size = 10, companyId = 1L)

            results shouldHaveSize 1
            results[0].post.company.id shouldBe "1"
        }

        test("rank는 1부터 시작하여 순서대로 부여된다") {
            val post2 = samplePost.copy(id = "post-2", title = "Kafka 도입기")
            val embedding2 = sampleEmbedding.copy(postId = "post-2")

            every { embeddingService.generateQuestionEmbedding(any()) } returns questionVector
            every {
                postEmbeddingRepository.findSimilarPosts(any(), any(), any())
            } returns listOf(sampleEmbedding, embedding2)
            every {
                postRepository.findAllByIdIn(listOf("post-1", "post-2"))
            } returns listOf(samplePost, post2)

            val results = service.search(query = "메시지 큐", size = 10, companyId = null)

            results shouldHaveSize 2
            results[0].rank shouldBe 1
            results[1].rank shouldBe 2
        }
    }
})
