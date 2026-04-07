package com.techinsights.domain.service.github

import com.techinsights.domain.config.cache.CacheConfig
import com.techinsights.domain.repository.github.GithubRepositoryRepository
import com.techinsights.domain.repository.github.GithubRepositoryWithDistance
import com.techinsights.domain.service.embedding.EmbeddingService
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(classes = [GithubSemanticSearchServiceCachingTest.CachingTestConfig::class, CacheConfig::class])
class GithubSemanticSearchServiceCachingTest : FunSpec() {

    override fun extensions() = listOf(SpringExtension)

    @Autowired
    private lateinit var service: GithubSemanticSearchService

    @Autowired
    private lateinit var cacheManager: CacheManager

    @Configuration
    open class CachingTestConfig {

        @Bean
        open fun embeddingService(): EmbeddingService = mockEmbeddingService

        @Bean
        open fun githubRepositoryRepository(): GithubRepositoryRepository = mockRepository

        @Bean
        open fun githubSemanticSearchService(
            embeddingService: EmbeddingService,
            repository: GithubRepositoryRepository,
        ) = GithubSemanticSearchServiceImpl(embeddingService, repository)
    }

    companion object {
        val mockEmbeddingService: EmbeddingService = mockk()
        val mockRepository: GithubRepositoryRepository = mockk()

        fun buildProjection(): GithubRepositoryWithDistance = mockk {
            every { fullName } returns "owner/repo"
            every { repoName } returns "repo"
            every { description } returns "desc"
            every { readmeSummary } returns "summary"
            every { primaryLanguage } returns "Kotlin"
            every { starCount } returns 1000L
            every { ownerName } returns "owner"
            every { ownerAvatarUrl } returns null
            every { topics } returns "kotlin,spring"
            every { htmlUrl } returns "https://github.com/owner/repo"
            every { distance } returns 0.5
        }
    }

    init {
        beforeTest {
            clearMocks(mockEmbeddingService, mockRepository)
            cacheManager.getCache(CacheConfig.GITHUB_SEARCH)?.clear()
        }

        test("동일한 쿼리로 두 번 검색 시 embeddingService는 한 번만 호출된다") {
            every { mockEmbeddingService.generateQuestionEmbedding("kotlin coroutine") } returns listOf(0.1f, 0.2f)
            every { mockRepository.findSimilarRepositories(any(), any()) } returns listOf(buildProjection())

            service.search("kotlin coroutine", 5)
            service.search("kotlin coroutine", 5)

            verify(exactly = 1) { mockEmbeddingService.generateQuestionEmbedding("kotlin coroutine") }
        }

        test("동일한 쿼리로 두 번 검색 시 repository는 한 번만 호출된다") {
            every { mockEmbeddingService.generateQuestionEmbedding("spring boot") } returns listOf(0.3f, 0.4f)
            every { mockRepository.findSimilarRepositories(any(), any()) } returns listOf(buildProjection())

            service.search("spring boot", 5)
            service.search("spring boot", 5)

            verify(exactly = 1) { mockRepository.findSimilarRepositories(any(), any()) }
        }

        test("쿼리가 다르면 각각 embeddingService를 호출한다") {
            every { mockEmbeddingService.generateQuestionEmbedding("kotlin") } returns listOf(0.1f)
            every { mockEmbeddingService.generateQuestionEmbedding("java") } returns listOf(0.2f)
            every { mockRepository.findSimilarRepositories(any(), any()) } returns emptyList()

            service.search("kotlin", 5)
            service.search("java", 5)

            verify(exactly = 1) { mockEmbeddingService.generateQuestionEmbedding("kotlin") }
            verify(exactly = 1) { mockEmbeddingService.generateQuestionEmbedding("java") }
        }
    }
}
