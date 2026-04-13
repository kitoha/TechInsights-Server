package com.techinsights.domain.service.github

import com.techinsights.domain.config.cache.CacheConfig
import com.techinsights.domain.dto.github.GithubRepositoryCursorPage
import com.techinsights.domain.dto.github.GithubRepositoryDto
import com.techinsights.domain.enums.GithubSortType
import com.techinsights.domain.repository.github.GithubRepositoryRepository
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
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDateTime

@ContextConfiguration(classes = [GithubTrendingServiceCachingTest.CachingTestConfig::class, CacheConfig::class])
class GithubTrendingServiceCachingTest : FunSpec() {

    override fun extensions() = listOf(SpringExtension)

    @Autowired
    private lateinit var service: GithubTrendingService

    @Autowired
    private lateinit var cacheManager: CacheManager

    @Configuration
    open class CachingTestConfig {

        @Bean
        open fun githubRepositoryRepository(): GithubRepositoryRepository = mockRepo

        @Bean
        open fun githubTrendingService(repo: GithubRepositoryRepository) = GithubTrendingService(repo)
    }

    companion object {
        val mockRepo: GithubRepositoryRepository = mockk()

        val sampleDto = GithubRepositoryDto(
            id = 1L,
            repoName = "spring-boot",
            fullName = "spring-projects/spring-boot",
            description = "Spring Boot",
            htmlUrl = "https://github.com/spring-projects/spring-boot",
            starCount = 75_000L,
            forkCount = 42_000L,
            primaryLanguage = "Java",
            ownerName = "spring-projects",
            ownerAvatarUrl = null,
            topics = listOf("spring", "java"),
            pushedAt = LocalDateTime.now(),
            fetchedAt = LocalDateTime.now(),
            weeklyStarDelta = 200L,
            dailyStarDelta = 30L,
            readmeSummary = null,
        )
    }

    init {
        beforeTest {
            clearMocks(mockRepo)
            cacheManager.getCache(CacheConfig.GITHUB_TRENDING)?.clear()
        }

        test("동일한 파라미터로 두 번 호출 시 repository는 한 번만 호출된다") {
            val pageable = PageRequest.of(0, 20)
            every {
                mockRepo.findRepositories(pageable, GithubSortType.STARS, null)
            } returns PageImpl(listOf(sampleDto))

            service.getRepositories(0, 20, GithubSortType.STARS, null)
            service.getRepositories(0, 20, GithubSortType.STARS, null)

            verify(exactly = 1) {
                mockRepo.findRepositories(pageable, GithubSortType.STARS, null)
            }
        }

        test("파라미터가 다르면 repository를 각각 호출한다") {
            val pageableFirst = PageRequest.of(0, 20)
            val pageableSecond = PageRequest.of(1, 20)
            every {
                mockRepo.findRepositories(pageableFirst, GithubSortType.STARS, null)
            } returns PageImpl(listOf(sampleDto))
            every {
                mockRepo.findRepositories(pageableSecond, GithubSortType.STARS, null)
            } returns PageImpl(listOf(sampleDto))

            service.getRepositories(0, 20, GithubSortType.STARS, null)
            service.getRepositories(1, 20, GithubSortType.STARS, null)

            verify(exactly = 1) {
                mockRepo.findRepositories(pageableFirst, GithubSortType.STARS, null)
            }
            verify(exactly = 1) {
                mockRepo.findRepositories(pageableSecond, GithubSortType.STARS, null)
            }
        }

        test("동일한 cursor 파라미터로 두 번 호출 시 repository는 한 번만 호출된다") {
            every {
                mockRepo.findRepositoriesByCursor(21, GithubSortType.STARS, null, null)
            } returns listOf(sampleDto)

            service.getRepositoriesByCursor(null, 20, GithubSortType.STARS, null)
            service.getRepositoriesByCursor(null, 20, GithubSortType.STARS, null)

            verify(exactly = 1) {
                mockRepo.findRepositoriesByCursor(21, GithubSortType.STARS, null, null)
            }
        }
    }
}
