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

    val tossCompany = CompanyDto(
        id = "1", name = "Toss", blogUrl = "https://toss.tech",
        logoImageName = "toss.png", rssSupported = true,
        totalViewCount = 1000, postCount = 50
    )

    val kakaoCompany = CompanyDto(
        id = "2", name = "Kakao", blogUrl = "https://kakao.tech",
        logoImageName = "kakao.png", rssSupported = true,
        totalViewCount = 2000, postCount = 80
    )

    val samplePost = PostDto(
        id = "post-1", title = "토스 MSA 전환기",
        preview = "토스는 MSA 전환을 단계적으로 진행했다.",
        url = "https://toss.tech/msa",
        content = "## MSA 전환\n- gRPC 게이트웨이 도입\n- 도메인 분리",
        publishedAt = LocalDateTime.now(), thumbnail = null,
        company = tossCompany, viewCount = 500,
        categories = setOf(Category.BackEnd), isSummary = true, isEmbedding = true
    )

    val sampleEmbedding = PostEmbeddingDto(
        postId = "post-1", companyName = "Toss", categories = "BackEnd",
        content = "토스는 MSA 전환을 단계적으로 진행했다.",
        embeddingVector = FloatArray(0),
        distance = 0.08,
    )

    val questionVector = List(3072) { 0.1f }

    beforeTest { clearAllMocks() }

    context("search") {

        test("질문과 관련된 게시글 목록을 반환한다") {
            every { embeddingService.generateQuestionEmbedding(any()) } returns questionVector
            every { postEmbeddingRepository.findSimilarPostsAll(any(), any()) } returns listOf(sampleEmbedding)
            every { postRepository.findAllByIdIn(listOf("post-1")) } returns listOf(samplePost)

            val results = service.search(query = "토스 MSA 전환", size = 10, companyId = null)

            results shouldHaveSize 1
            results[0].post.title shouldBe "토스 MSA 전환기"
            results[0].rank shouldBe 1
        }

        test("유사도 점수는 0.0 초과 1.0 이하여야 한다") {
            every { embeddingService.generateQuestionEmbedding(any()) } returns questionVector
            every { postEmbeddingRepository.findSimilarPostsAll(any(), any()) } returns listOf(sampleEmbedding)
            every { postRepository.findAllByIdIn(any()) } returns listOf(samplePost)

            val results = service.search(query = "MSA", size = 10, companyId = null)

            results[0].similarityScore shouldBeGreaterThan 0.0
            results[0].similarityScore shouldBeLessThanOrEqualTo 1.0
        }

        test("유사도 점수는 DB에서 반환된 실제 distance를 기반으로 계산된다") {
            val embeddingWithDistance = sampleEmbedding.copy(distance = 0.25)
            every { embeddingService.generateQuestionEmbedding(any()) } returns questionVector
            every { postEmbeddingRepository.findSimilarPostsAll(any(), any()) } returns listOf(embeddingWithDistance)
            every { postRepository.findAllByIdIn(any()) } returns listOf(samplePost)

            val results = service.search(query = "MSA", size = 10, companyId = null)

            // similarityScore = 1.0 / (1.0 + distance) = 1.0 / 1.25 = 0.8
            results[0].similarityScore shouldBe (1.0 / (1.0 + 0.25))
        }

        test("distance가 클수록 similarityScore는 낮아진다") {
            val closeEmbedding = sampleEmbedding.copy(postId = "post-1", distance = 0.1)
            val farEmbedding = sampleEmbedding.copy(postId = "post-2", distance = 0.9)
            val post2 = samplePost.copy(id = "post-2")

            every { embeddingService.generateQuestionEmbedding(any()) } returns questionVector
            every { postEmbeddingRepository.findSimilarPostsAll(any(), any()) } returns listOf(closeEmbedding, farEmbedding)
            every { postRepository.findAllByIdIn(any()) } returns listOf(samplePost, post2)

            val results = service.search(query = "MSA", size = 10, companyId = null)

            results[0].similarityScore shouldBeGreaterThan results[1].similarityScore
        }

        test("관련 게시글이 없으면 빈 목록을 반환한다") {
            every { embeddingService.generateQuestionEmbedding(any()) } returns questionVector
            every { postEmbeddingRepository.findSimilarPostsAll(any(), any()) } returns emptyList()

            val results = service.search(query = "없는 주제", size = 10, companyId = null)

            results.shouldBeEmpty()
            verify(exactly = 0) { postRepository.findAllByIdIn(any()) }
        }

        test("companyId 없을 때 limit은 resolvedSize 그대로 전달된다") {
            every { embeddingService.generateQuestionEmbedding(any()) } returns questionVector
            every { postEmbeddingRepository.findSimilarPostsAll(any(), 5L) } returns emptyList()

            service.search(query = "쿠버네티스", size = 5, companyId = null)

            verify(exactly = 1) { postEmbeddingRepository.findSimilarPostsAll(any(), 5L) }
        }

        test("companyId 있을 때 limit은 resolvedSize * 3으로 over-fetch된다") {
            every { embeddingService.generateQuestionEmbedding(any()) } returns questionVector
            every { postEmbeddingRepository.findSimilarPostsAll(any(), 15L) } returns emptyList()

            service.search(query = "쿠버네티스", size = 5, companyId = 1L)

            verify(exactly = 1) { postEmbeddingRepository.findSimilarPostsAll(any(), 15L) }
        }

        test("companyId 필터가 있으면 해당 회사 게시글만 반환한다") {
            val otherPost = samplePost.copy(id = "post-2", company = kakaoCompany)
            val otherEmbedding = sampleEmbedding.copy(postId = "post-2", companyName = "Kakao")

            every { embeddingService.generateQuestionEmbedding(any()) } returns questionVector
            every { postEmbeddingRepository.findSimilarPostsAll(any(), any()) } returns listOf(sampleEmbedding, otherEmbedding)
            every { postRepository.findAllByIdIn(any()) } returns listOf(samplePost, otherPost)

            val results = service.search(query = "MSA", size = 10, companyId = 1L)

            results shouldHaveSize 1
            results[0].post.company.id shouldBe "1"
        }

        test("rank는 embedding 유사도 순서를 보존한다 (DB 반환 순서가 달라도)") {
            val post2 = samplePost.copy(id = "post-2", title = "Kafka 도입기")
            val embedding1 = sampleEmbedding.copy(postId = "post-1") // 1순위
            val embedding2 = sampleEmbedding.copy(postId = "post-2") // 2순위

            every { embeddingService.generateQuestionEmbedding(any()) } returns questionVector
            every { postEmbeddingRepository.findSimilarPostsAll(any(), any()) } returns listOf(embedding1, embedding2)
            // DB가 embedding 순서와 반대로 반환
            every { postRepository.findAllByIdIn(any()) } returns listOf(post2, samplePost)

            val results = service.search(query = "메시지 큐", size = 10, companyId = null)

            results shouldHaveSize 2
            results[0].rank shouldBe 1
            results[0].post.id shouldBe "post-1" // embedding 1순위가 rank 1
            results[1].rank shouldBe 2
            results[1].post.id shouldBe "post-2"
        }

        test("company.id가 숫자가 아니면 companyId 필터에서 제외된다") {
            val invalidCompanyPost = samplePost.copy(
                id = "post-2",
                company = tossCompany.copy(id = "invalid-id")
            )
            val invalidEmbedding = sampleEmbedding.copy(postId = "post-2")

            every { embeddingService.generateQuestionEmbedding(any()) } returns questionVector
            every { postEmbeddingRepository.findSimilarPostsAll(any(), any()) } returns listOf(sampleEmbedding, invalidEmbedding)
            every { postRepository.findAllByIdIn(any()) } returns listOf(samplePost, invalidCompanyPost)

            val results = service.search(query = "MSA", size = 10, companyId = 1L)

            results shouldHaveSize 1
            results[0].post.id shouldBe "post-1"
        }
    }
})
