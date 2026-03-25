package com.techinsights.batch.github.readme.reader

import com.techinsights.batch.github.readme.config.props.GithubReadmeBatchProperties
import com.techinsights.domain.dto.github.GithubRepositoryDto
import com.techinsights.domain.enums.ErrorType
import com.techinsights.domain.repository.github.GithubRepositoryRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime

class UnsummarizedRepoReaderTest : FunSpec({

    val repository = mockk<GithubRepositoryRepository>()
    val properties = mockk<GithubReadmeBatchProperties>()

    beforeTest { clearAllMocks() }

    test("첫 번째 read() 호출 시 DB에서 페이지를 로드하고 첫 항목을 반환한다") {
        val repo1 = createDto("owner/repo1", starCount = 1000L, id = 1L)
        val repo2 = createDto("owner/repo2", starCount = 900L, id = 2L)
        every { repository.findUnsummarized(any(), isNull(), isNull(), any(), any()) } returns listOf(repo1, repo2)
        every { properties.pageSize } returns 10
        every { properties.maxItemsPerRun } returns Int.MAX_VALUE
        every { properties.retryEnabled } returns false

        val reader = UnsummarizedRepoReader(repository, properties, maxItemsPerRunParam = null)

        reader.read() shouldBe repo1
        reader.read() shouldBe repo2
    }

    test("버퍼가 소진되면 다음 페이지를 커서 기반으로 조회한다") {
        val repo1 = createDto("owner/repo1", starCount = 1000L, id = 1L)
        val repo2 = createDto("owner/repo2", starCount = 500L, id = 2L)

        every { repository.findUnsummarized(2, null, null, any(), any()) } returns listOf(repo1)
        every { repository.findUnsummarized(2, 1000L, 1L, any(), any()) } returns listOf(repo2)
        every { repository.findUnsummarized(2, 500L, 2L, any(), any()) } returns emptyList()
        every { properties.pageSize } returns 2
        every { properties.maxItemsPerRun } returns Int.MAX_VALUE
        every { properties.retryEnabled } returns false

        val reader = UnsummarizedRepoReader(repository, properties, maxItemsPerRunParam = null)

        reader.read() shouldBe repo1
        reader.read() shouldBe repo2
        reader.read().shouldBeNull()
    }

    test("DB가 빈 리스트를 반환하면 null을 반환한다") {
        every { repository.findUnsummarized(any(), isNull(), isNull(), any(), any()) } returns emptyList()
        every { properties.pageSize } returns 10
        every { properties.maxItemsPerRun } returns Int.MAX_VALUE
        every { properties.retryEnabled } returns false

        val reader = UnsummarizedRepoReader(repository, properties, maxItemsPerRunParam = null)

        reader.read().shouldBeNull()
    }

    test("소진 후 read()를 다시 호출해도 DB를 추가 조회하지 않는다") {
        every { repository.findUnsummarized(any(), isNull(), isNull(), any(), any()) } returns emptyList()
        every { properties.pageSize } returns 10
        every { properties.maxItemsPerRun } returns Int.MAX_VALUE
        every { properties.retryEnabled } returns false

        val reader = UnsummarizedRepoReader(repository, properties, maxItemsPerRunParam = null)

        reader.read().shouldBeNull()
        reader.read().shouldBeNull()

        verify(exactly = 1) { repository.findUnsummarized(any(), any(), any(), any(), any()) }
    }

    test("페이지 크기만큼 정확히 조회한다") {
        every { repository.findUnsummarized(50, null, null, any(), any()) } returns emptyList()
        every { properties.pageSize } returns 50
        every { properties.maxItemsPerRun } returns Int.MAX_VALUE
        every { properties.retryEnabled } returns false

        val reader = UnsummarizedRepoReader(repository, properties, maxItemsPerRunParam = null)
        reader.read()

        verify { repository.findUnsummarized(50, null, null, any(), any()) }
    }

    test("retryEnabled=true 시 retryAfter와 RETRYABLE_TYPES가 전달된다") {
        every { repository.findUnsummarized(any(), isNull(), isNull(), any(), eq(ErrorType.RETRYABLE_TYPES)) } returns emptyList()
        every { properties.pageSize } returns 10
        every { properties.maxItemsPerRun } returns Int.MAX_VALUE
        every { properties.retryEnabled } returns true
        every { properties.retryAfterDays } returns 7L

        val reader = UnsummarizedRepoReader(repository, properties, maxItemsPerRunParam = null)
        reader.read()

        verify { repository.findUnsummarized(any(), isNull(), isNull(), any<LocalDateTime>(), eq(ErrorType.RETRYABLE_TYPES)) }
    }

    test("retryEnabled=false 시 retryAfter=null로 전달된다") {
        every { repository.findUnsummarized(any(), isNull(), isNull(), isNull(), eq(emptySet())) } returns emptyList()
        every { properties.pageSize } returns 10
        every { properties.maxItemsPerRun } returns Int.MAX_VALUE
        every { properties.retryEnabled } returns false

        val reader = UnsummarizedRepoReader(repository, properties, maxItemsPerRunParam = null)
        reader.read()

        verify { repository.findUnsummarized(any(), isNull(), isNull(), isNull(), eq(emptySet())) }
    }
})

private fun createDto(
    fullName: String,
    starCount: Long = 100L,
    id: Long = fullName.hashCode().toLong(),
) = GithubRepositoryDto(
    id = id,
    repoName = fullName.substringAfter("/"),
    fullName = fullName,
    description = null,
    htmlUrl = "https://github.com/$fullName",
    starCount = starCount,
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
