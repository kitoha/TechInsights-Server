package com.techinsights.api.github

import com.techinsights.domain.dto.github.GithubSemanticSearchResult
import com.techinsights.domain.service.github.GithubSemanticSearchService
import com.techinsights.domain.service.github.GithubTrendingService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import org.springframework.data.domain.PageImpl

class GithubSemanticSearchControllerTest : FunSpec() {

    private val githubTrendingService = mockk<GithubTrendingService>(relaxed = true)
    private val githubSemanticSearchService = mockk<GithubSemanticSearchService>()
    private val ioDispatcher = Dispatchers.Unconfined

    private val controller = GithubTrendingController(
        githubTrendingService = githubTrendingService,
        githubSemanticSearchService = githubSemanticSearchService,
        ioDispatcher = ioDispatcher,
    )

    init {
        beforeTest {
            clearAllMocks()
            every { githubTrendingService.getRepositories(any(), any(), any(), any()) } returns PageImpl(emptyList())
        }

        test("GET /api/v1/github/search - 결과를 포함한 200 응답을 반환한다") {
            val results = listOf(createMockResult(1), createMockResult(2))
            every { githubSemanticSearchService.search("Kotlin 서버", 10) } returns results

            val response = controller.searchRepos(query = "Kotlin 서버", size = 10)

            response.statusCode.value() shouldBe 200
            val body = response.body!!
            body.query shouldBe "Kotlin 서버"
            body.results shouldHaveSize 2
            body.results[0].rank shouldBe 1
            body.results[0].fullName shouldBe "octocat/repo-1"
            body.totalReturned shouldBe 2

            verify(exactly = 1) { githubSemanticSearchService.search("Kotlin 서버", 10) }
        }

        test("GET /api/v1/github/search - 기본 size=10 으로 서비스에 전달한다") {
            every { githubSemanticSearchService.search(any(), 10) } returns emptyList()

            val response = controller.searchRepos(query = "Rust 웹 프레임워크", size = 10)

            response.statusCode.value() shouldBe 200
            verify(exactly = 1) { githubSemanticSearchService.search("Rust 웹 프레임워크", 10) }
        }

        test("GET /api/v1/github/search - 커스텀 size를 서비스에 전달한다") {
            every { githubSemanticSearchService.search("React", 5) } returns listOf(createMockResult(1))

            val response = controller.searchRepos(query = "React", size = 5)

            response.statusCode.value() shouldBe 200
            verify(exactly = 1) { githubSemanticSearchService.search("React", 5) }
        }

        test("GET /api/v1/github/search - 결과가 없을 때 빈 리스트를 반환한다") {
            every { githubSemanticSearchService.search(any(), any()) } returns emptyList()

            val response = controller.searchRepos(query = "존재하지않는주제", size = 10)

            response.statusCode.value() shouldBe 200
            val body = response.body!!
            body.results.shouldBeEmpty()
            body.totalReturned shouldBe 0
        }

        test("GET /api/v1/github/search - 앞뒤 공백을 제거한 query를 서비스에 전달한다") {
            val results = listOf(createMockResult(1))
            every { githubSemanticSearchService.search("Spring Boot", 10) } returns results

            val response = controller.searchRepos(query = "  Spring Boot  ", size = 10)

            response.statusCode.value() shouldBe 200
            val body = response.body!!
            body.query shouldBe "Spring Boot"
            verify(exactly = 1) { githubSemanticSearchService.search("Spring Boot", 10) }
        }

        test("GET /api/v1/github/search - processingTimeMs 가 0 이상이다") {
            every { githubSemanticSearchService.search(any(), any()) } returns emptyList()

            val response = controller.searchRepos(query = "Docker", size = 10)

            val body = response.body!!
            (body.processingTimeMs >= 0) shouldBe true
        }

        test("GET /api/v1/github/search - similarityScore 와 rank 가 응답에 포함된다") {
            val result = createMockResult(rank = 1, similarityScore = 0.95)
            every { githubSemanticSearchService.search(any(), any()) } returns listOf(result)

            val response = controller.searchRepos(query = "Go 언어", size = 10)

            val body = response.body!!
            body.results[0].similarityScore shouldBe 0.95
            body.results[0].rank shouldBe 1
        }
    }

    private fun createMockResult(
        rank: Int,
        similarityScore: Double = 0.9 - rank * 0.05,
    ): GithubSemanticSearchResult = GithubSemanticSearchResult(
        fullName = "octocat/repo-$rank",
        repoName = "repo-$rank",
        description = "Test repo $rank",
        readmeSummary = "Summary for repo $rank",
        primaryLanguage = "Kotlin",
        starCount = 1000L * rank,
        ownerName = "octocat",
        ownerAvatarUrl = "https://avatars.githubusercontent.com/u/583231",
        topics = listOf("kotlin", "test"),
        htmlUrl = "https://github.com/octocat/repo-$rank",
        similarityScore = similarityScore,
        rank = rank,
    )
}
