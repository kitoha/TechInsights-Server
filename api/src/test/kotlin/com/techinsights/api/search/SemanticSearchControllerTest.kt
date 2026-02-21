package com.techinsights.api.search

import com.techinsights.domain.config.search.SemanticSearchProperties
import com.techinsights.domain.dto.company.CompanyDto
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.dto.search.SemanticSearchResult
import com.techinsights.domain.enums.Category
import com.techinsights.domain.service.search.SemanticSearchService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import java.time.LocalDateTime

class SemanticSearchControllerTest : FunSpec() {

    private val semanticSearchService = mockk<SemanticSearchService>()
    private val properties = SemanticSearchProperties(defaultSize = 10, maxSize = 20)
    private val ioDispatcher = Dispatchers.Unconfined

    private val controller = SearchController(
        searchService = mockk(relaxed = true),
        semanticSearchService = semanticSearchService,
        properties = properties,
        ioDispatcher = ioDispatcher
    )

    private val sampleCompany = CompanyDto(
        id = "1", name = "Toss", blogUrl = "https://toss.tech",
        logoImageName = "toss.png", rssSupported = true,
        totalViewCount = 1000, postCount = 50
    )

    private val samplePost = PostDto(
        id = "post-1", title = "토스 MSA 전환기",
        preview = "토스는 MSA 전환을 단계적으로 진행했다.",
        url = "https://toss.tech/msa", content = "## MSA 전환",
        publishedAt = LocalDateTime.of(2024, 1, 1, 0, 0),
        thumbnail = null, company = sampleCompany, viewCount = 500,
        categories = setOf(Category.BackEnd), isSummary = true, isEmbedding = true
    )

    private val sampleResult = SemanticSearchResult(
        post = samplePost,
        similarityScore = 0.91,
        rank = 1
    )

    init {
        beforeTest {
            clearAllMocks()
        }

        test("semanticSearch - should return 200 with results") {
            every {
                semanticSearchService.search("MSA 전환", 10, null)
            } returns listOf(sampleResult)

            val response = controller.semanticSearch(
                query = "MSA 전환",
                size = null,
                companyId = null
            )

            response.statusCode.value() shouldBe 200
            val body = response.body!!
            body.query shouldBe "MSA 전환"
            body.results shouldHaveSize 1
            body.results[0].rank shouldBe 1
            body.results[0].post.title shouldBe "토스 MSA 전환기"
            body.totalReturned shouldBe 1

            verify(exactly = 1) { semanticSearchService.search("MSA 전환", 10, null) }
        }

        test("semanticSearch - should use defaultSize when size not provided") {
            every { semanticSearchService.search(any(), 10, null) } returns emptyList()

            val response = controller.semanticSearch(
                query = "Kubernetes",
                size = null,
                companyId = null
            )

            response.statusCode.value() shouldBe 200
            verify(exactly = 1) { semanticSearchService.search(any(), 10, null) }
        }

        test("semanticSearch - should pass companyId filter") {
            every { semanticSearchService.search("MSA", 10, 1L) } returns listOf(sampleResult)

            val response = controller.semanticSearch(
                query = "MSA",
                size = null,
                companyId = 1L
            )

            response.statusCode.value() shouldBe 200
            verify(exactly = 1) { semanticSearchService.search("MSA", 10, 1L) }
        }

        test("semanticSearch - should return empty results without error") {
            every { semanticSearchService.search(any(), any(), any()) } returns emptyList()

            val response = controller.semanticSearch(
                query = "없는주제",
                size = null,
                companyId = null
            )

            response.statusCode.value() shouldBe 200
            val body = response.body!!
            body.results.shouldBeEmpty()
            body.totalReturned shouldBe 0
        }

        test("semanticSearch - should use custom size when provided") {
            every { semanticSearchService.search("Kafka", 5, null) } returns emptyList()

            val response = controller.semanticSearch(
                query = "Kafka",
                size = 5,
                companyId = null
            )

            response.statusCode.value() shouldBe 200
            verify(exactly = 1) { semanticSearchService.search("Kafka", 5, null) }
        }

        test("semanticSearch - should trim whitespace from query") {
            every { semanticSearchService.search("MSA", 10, null) } returns listOf(sampleResult)

            val response = controller.semanticSearch(
                query = "  MSA  ",
                size = null,
                companyId = null
            )

            response.statusCode.value() shouldBe 200
            val body = response.body!!
            body.query shouldBe "MSA"
            verify(exactly = 1) { semanticSearchService.search("MSA", 10, null) }
        }
    }
}
