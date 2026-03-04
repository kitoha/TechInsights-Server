package com.techinsights.domain.service.github

import com.techinsights.domain.repository.github.GithubRepositoryRepository
import com.techinsights.domain.repository.github.GithubRepositoryWithDistance
import com.techinsights.domain.service.embedding.EmbeddingService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class GithubSemanticSearchServiceImplTest : FunSpec({

    val embeddingService = mockk<EmbeddingService>()
    val githubRepositoryRepository = mockk<GithubRepositoryRepository>()
    val service = GithubSemanticSearchServiceImpl(embeddingService, githubRepositoryRepository)

    beforeTest { clearAllMocks() }

    fun buildProjection(
        fullName: String = "owner/repo",
        repoName: String = "repo",
        description: String? = "desc",
        readmeSummary: String? = "summary",
        primaryLanguage: String? = "Kotlin",
        starCount: Long = 1000L,
        ownerName: String = "owner",
        ownerAvatarUrl: String? = null,
        topics: String? = "kotlin,test",
        htmlUrl: String = "https://github.com/owner/repo",
        distance: Double = 0.5,
    ): GithubRepositoryWithDistance = mockk {
        every { this@mockk.fullName } returns fullName
        every { this@mockk.repoName } returns repoName
        every { this@mockk.description } returns description
        every { this@mockk.readmeSummary } returns readmeSummary
        every { this@mockk.primaryLanguage } returns primaryLanguage
        every { this@mockk.starCount } returns starCount
        every { this@mockk.ownerName } returns ownerName
        every { this@mockk.ownerAvatarUrl } returns ownerAvatarUrl
        every { this@mockk.topics } returns topics
        every { this@mockk.htmlUrl } returns htmlUrl
        every { this@mockk.distance } returns distance
    }

    test("쿼리를 임베딩하고 유사 레포지토리를 반환한다") {
        val vector = listOf(0.1f, 0.2f, 0.3f)
        val projection = buildProjection(distance = 0.5)

        every { embeddingService.generateQuestionEmbedding("AI CLI 도구") } returns vector
        every { githubRepositoryRepository.findSimilarRepositories("[0.1,0.2,0.3]", 5L) } returns listOf(projection)

        val result = service.search("AI CLI 도구", 5)

        result shouldHaveSize 1
        result[0].rank shouldBe 1
        result[0].similarityScore shouldBe (1.0 / 1.5 plusOrMinus 1e-9)
        result[0].fullName shouldBe "owner/repo"
    }

    test("벡터를 pgvector 문자열 형식으로 변환하여 레포지토리를 조회한다") {
        val vector = listOf(0.1f, 0.2f)

        every { embeddingService.generateQuestionEmbedding(any()) } returns vector
        every { githubRepositoryRepository.findSimilarRepositories("[0.1,0.2]", any()) } returns emptyList()

        service.search("test", 5)

        verify { githubRepositoryRepository.findSimilarRepositories("[0.1,0.2]", 5L) }
    }

    test("size가 20을 초과하면 20으로 제한한다") {
        every { embeddingService.generateQuestionEmbedding(any()) } returns listOf(0.1f)
        every { githubRepositoryRepository.findSimilarRepositories(any(), 20L) } returns emptyList()

        service.search("test", 100)

        verify { githubRepositoryRepository.findSimilarRepositories(any(), 20L) }
    }

    test("결과가 없으면 빈 리스트를 반환한다") {
        every { embeddingService.generateQuestionEmbedding(any()) } returns listOf(0.1f)
        every { githubRepositoryRepository.findSimilarRepositories(any(), any()) } returns emptyList()

        val result = service.search("없는 검색어", 5)

        result.shouldBeEmpty()
    }

    test("여러 결과의 rank가 1부터 순서대로 부여된다") {
        val projections = (1..3).map { i -> buildProjection(fullName = "owner/repo$i", distance = i * 0.1) }

        every { embeddingService.generateQuestionEmbedding(any()) } returns listOf(0.1f)
        every { githubRepositoryRepository.findSimilarRepositories(any(), any()) } returns projections

        val result = service.search("test", 3)

        result.map { it.rank } shouldBe listOf(1, 2, 3)
    }

    test("topics 쉼표 문자열을 trim한 리스트로 변환한다") {
        val projection = buildProjection(topics = "kotlin, spring, batch")

        every { embeddingService.generateQuestionEmbedding(any()) } returns listOf(0.1f)
        every { githubRepositoryRepository.findSimilarRepositories(any(), any()) } returns listOf(projection)

        val result = service.search("test", 5)

        result[0].topics shouldBe listOf("kotlin", "spring", "batch")
    }

    test("topics가 null이면 빈 리스트를 반환한다") {
        val projection = buildProjection(topics = null)

        every { embeddingService.generateQuestionEmbedding(any()) } returns listOf(0.1f)
        every { githubRepositoryRepository.findSimilarRepositories(any(), any()) } returns listOf(projection)

        val result = service.search("test", 5)

        result[0].topics.shouldBeEmpty()
    }

    test("distance가 0이면 similarityScore가 1.0이다") {
        val projection = buildProjection(distance = 0.0)

        every { embeddingService.generateQuestionEmbedding(any()) } returns listOf(0.1f)
        every { githubRepositoryRepository.findSimilarRepositories(any(), any()) } returns listOf(projection)

        val result = service.search("test", 5)

        result[0].similarityScore shouldBe 1.0
    }
})
