package com.techinsights.batch.github.community.analyze.reader

import com.techinsights.batch.github.community.analyze.config.props.CommunityAnalyzeBatchProperties
import com.techinsights.domain.dto.github.GithubRepositoryDto
import com.techinsights.domain.repository.github.GithubRepositoryRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime

class CommunityAnalyzeReaderTest : FunSpec({

    val repository = mockk<GithubRepositoryRepository>()
    val properties = mockk<CommunityAnalyzeBatchProperties>()

    beforeTest { clearAllMocks() }

    test("첫 번째 read() 호출 시 DB에서 페이지를 로드하고 첫 항목을 반환한다") {
        val repo1 = createDto("owner/repo1", id = 1L)
        val repo2 = createDto("owner/repo2", id = 2L)
        every { repository.findForCommunityAnalyze(any(), isNull()) } returns listOf(repo1, repo2)
        every { properties.pageSize } returns 10
        every { properties.maxItemsPerRun } returns Int.MAX_VALUE

        val reader = CommunityAnalyzeReader(repository, properties)

        reader.read() shouldBe repo1
        reader.read() shouldBe repo2
    }

    test("버퍼가 소진되면 다음 페이지를 afterId 커서로 조회한다") {
        val repo1 = createDto("owner/repo1", id = 1L)
        val repo2 = createDto("owner/repo2", id = 2L)

        every { repository.findForCommunityAnalyze(2, null) } returns listOf(repo1)
        every { repository.findForCommunityAnalyze(2, 1L) } returns listOf(repo2)
        every { repository.findForCommunityAnalyze(2, 2L) } returns emptyList()
        every { properties.pageSize } returns 2
        every { properties.maxItemsPerRun } returns Int.MAX_VALUE

        val reader = CommunityAnalyzeReader(repository, properties)

        reader.read() shouldBe repo1
        reader.read() shouldBe repo2
        reader.read().shouldBeNull()
    }

    test("maxItemsPerRun 도달 시 null을 반환한다") {
        val repo1 = createDto("owner/repo1", id = 1L)
        val repo2 = createDto("owner/repo2", id = 2L)

        every { repository.findForCommunityAnalyze(any(), isNull()) } returns listOf(repo1, repo2)
        every { properties.pageSize } returns 10
        every { properties.maxItemsPerRun } returns 1

        val reader = CommunityAnalyzeReader(repository, properties)

        reader.read() shouldBe repo1
        reader.read().shouldBeNull()
    }

    test("DB 결과가 비면 null을 반환한다") {
        every { repository.findForCommunityAnalyze(any(), isNull()) } returns emptyList()
        every { properties.pageSize } returns 10
        every { properties.maxItemsPerRun } returns Int.MAX_VALUE

        val reader = CommunityAnalyzeReader(repository, properties)

        reader.read().shouldBeNull()
    }

    test("소진 후 read()를 다시 호출해도 DB를 추가 조회하지 않는다") {
        every { repository.findForCommunityAnalyze(any(), isNull()) } returns emptyList()
        every { properties.pageSize } returns 10
        every { properties.maxItemsPerRun } returns Int.MAX_VALUE

        val reader = CommunityAnalyzeReader(repository, properties)

        reader.read().shouldBeNull()
        reader.read().shouldBeNull()

        verify(exactly = 1) { repository.findForCommunityAnalyze(any(), any()) }
    }

    test("커서가 마지막 항목의 id로 업데이트된다") {
        val repo1 = createDto("owner/repo1", id = 10L)
        val repo2 = createDto("owner/repo2", id = 20L)

        every { repository.findForCommunityAnalyze(2, null) } returns listOf(repo1, repo2)
        every { repository.findForCommunityAnalyze(2, 20L) } returns emptyList()
        every { properties.pageSize } returns 2
        every { properties.maxItemsPerRun } returns Int.MAX_VALUE

        val reader = CommunityAnalyzeReader(repository, properties)

        repeat(3) { reader.read() }

        verify(exactly = 1) { repository.findForCommunityAnalyze(2, 20L) }
    }
})

private fun createDto(
    fullName: String,
    id: Long = fullName.hashCode().toLong(),
) = GithubRepositoryDto(
    id = id,
    repoName = fullName.substringAfter("/"),
    fullName = fullName,
    description = null,
    htmlUrl = "https://github.com/$fullName",
    starCount = 100L,
    forkCount = 10L,
    primaryLanguage = "Kotlin",
    ownerName = fullName.substringBefore("/"),
    ownerAvatarUrl = null,
    topics = emptyList(),
    pushedAt = LocalDateTime.now(),
    fetchedAt = LocalDateTime.now(),
    weeklyStarDelta = 0L,
    dailyStarDelta = 0L,
    readmeSummary = null,
)
