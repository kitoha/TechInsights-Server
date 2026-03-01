package com.techinsights.batch.github.reader

import com.techinsights.batch.github.config.props.GithubApiProperties
import com.techinsights.batch.github.config.props.GithubBatchProperties
import com.techinsights.batch.github.dto.GithubSearchResponse
import com.techinsights.batch.github.service.GithubTrendingFetchService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers

class GithubRepoReaderTest : FunSpec({

    val fetchService = mockk<GithubTrendingFetchService>()
    val apiProperties = GithubApiProperties(perPage = 2)
    /** 테스트에서는 Unconfined — 스레드 전환 없이 즉시 실행 */
    val testDispatcher = Dispatchers.Unconfined

    beforeTest { clearAllMocks() }

    test("첫 번째 read() 호출 시 fetchAll을 실행하고 items를 순서대로 반환한다") {
        val batchProperties = mockk<GithubBatchProperties>()
        every { batchProperties.queries } returns listOf(
            GithubBatchProperties.QueryConfig("stars:>100 created:2023-01-01..2024-12-31")
        )

        val item1 = createItem("owner/repo1")
        val item2 = createItem("owner/repo2")
        coEvery { fetchService.fetchPage("stars:>100 created:2023-01-01..2024-12-31", 1) } returns
            GithubSearchResponse(totalCount = 2, items = listOf(item1, item2))

        val reader = GithubRepoReader(fetchService, batchProperties, apiProperties, testDispatcher)

        reader.read() shouldBe item1
        reader.read() shouldBe item2
        reader.read().shouldBeNull()
    }

    test("중복된 fullName의 Item은 한 번만 반환된다") {
        val batchProperties = mockk<GithubBatchProperties>()
        every { batchProperties.queries } returns listOf(
            GithubBatchProperties.QueryConfig("query1"),
            GithubBatchProperties.QueryConfig("query2"),
        )

        val item = createItem("owner/same-repo")
        coEvery { fetchService.fetchPage("query1", 1) } returns
            GithubSearchResponse(totalCount = 1, items = listOf(item))
        coEvery { fetchService.fetchPage("query2", 1) } returns
            GithubSearchResponse(totalCount = 1, items = listOf(item))

        val reader = GithubRepoReader(fetchService, batchProperties, apiProperties, testDispatcher)

        val results = buildList<GithubSearchResponse.Item> {
            while (true) {
                val next = reader.read() ?: break
                add(next)
            }
        }

        results.size shouldBe 1
        results[0].fullName shouldBe "owner/same-repo"
    }

    test("하나의 쿼리가 실패해도 나머지 쿼리 결과를 반환한다") {
        val batchProperties = mockk<GithubBatchProperties>()
        every { batchProperties.queries } returns listOf(
            GithubBatchProperties.QueryConfig("query-fail"),
            GithubBatchProperties.QueryConfig("query-ok"),
        )

        val item = createItem("owner/ok-repo")
        coEvery { fetchService.fetchPage("query-fail", 1) } throws RuntimeException("Network error")
        coEvery { fetchService.fetchPage("query-ok", 1) } returns
            GithubSearchResponse(totalCount = 1, items = listOf(item))

        val reader = GithubRepoReader(fetchService, batchProperties, apiProperties, testDispatcher)

        val first = reader.read()
        first.shouldNotBeNull()
        first!!.fullName shouldBe "owner/ok-repo"
        reader.read().shouldBeNull()
    }

    test("totalCount 기반으로 총 페이지 수를 계산해 추가 페이지를 요청한다") {
        val batchProperties = mockk<GithubBatchProperties>()
        every { batchProperties.queries } returns listOf(
            GithubBatchProperties.QueryConfig("stars:>100")
        )

        val item1 = createItem("owner/repo1")
        val item2 = createItem("owner/repo2")
        val item3 = createItem("owner/repo3")

        // perPage=2, totalCount=3 → 2 pages
        coEvery { fetchService.fetchPage("stars:>100", 1) } returns
            GithubSearchResponse(totalCount = 3, items = listOf(item1, item2))
        coEvery { fetchService.fetchPage("stars:>100", 2) } returns
            GithubSearchResponse(totalCount = 3, items = listOf(item3))

        val reader = GithubRepoReader(fetchService, batchProperties, apiProperties, testDispatcher)

        val results = buildList<GithubSearchResponse.Item> {
            while (true) {
                val next = reader.read() ?: break
                add(next)
            }
        }

        results.size shouldBe 3
    }
})

private fun createItem(fullName: String = "owner/repo"): GithubSearchResponse.Item =
    GithubSearchResponse.Item(
        id = fullName.hashCode().toLong(),
        name = fullName.substringAfter("/"),
        fullName = fullName,
        description = "desc",
        htmlUrl = "https://github.com/$fullName",
        stargazersCount = 1000L,
        forksCount = 100L,
        language = "Kotlin",
        owner = GithubSearchResponse.Owner(
            login = fullName.substringBefore("/"),
            avatarUrl = null,
        ),
        topics = listOf("kotlin"),
        pushedAt = "2024-06-01T10:00:00Z",
    )
