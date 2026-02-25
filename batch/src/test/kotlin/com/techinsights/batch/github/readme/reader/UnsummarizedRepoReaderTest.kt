package com.techinsights.batch.github.readme.reader

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

class UnsummarizedRepoReaderTest : FunSpec({

    val repository = mockk<GithubRepositoryRepository>()

    beforeTest { clearAllMocks() }

    test("첫 번째 read() 호출 시 DB에서 페이지를 로드하고 첫 항목을 반환한다") {
        val repo1 = createDto("owner/repo1", starCount = 1000L, id = 1L)
        val repo2 = createDto("owner/repo2", starCount = 900L, id = 2L)
        every { repository.findUnsummarized(any(), isNull(), isNull()) } returns listOf(repo1, repo2)

        val reader = UnsummarizedRepoReader(repository, pageSize = 10)

        reader.read() shouldBe repo1
        reader.read() shouldBe repo2
    }

    test("버퍼가 소진되면 다음 페이지를 커서 기반으로 조회한다") {
        val repo1 = createDto("owner/repo1", starCount = 1000L, id = 1L)
        val repo2 = createDto("owner/repo2", starCount = 500L, id = 2L)

        every { repository.findUnsummarized(2, null, null) } returns listOf(repo1)
        every { repository.findUnsummarized(2, 1000L, 1L) } returns listOf(repo2)
        every { repository.findUnsummarized(2, 500L, 2L) } returns emptyList()

        val reader = UnsummarizedRepoReader(repository, pageSize = 2)

        reader.read() shouldBe repo1
        reader.read() shouldBe repo2
        reader.read().shouldBeNull()
    }

    test("DB가 빈 리스트를 반환하면 null을 반환한다") {
        every { repository.findUnsummarized(any(), isNull(), isNull()) } returns emptyList()

        val reader = UnsummarizedRepoReader(repository, pageSize = 10)

        reader.read().shouldBeNull()
    }

    test("소진 후 read()를 다시 호출해도 DB를 추가 조회하지 않는다") {
        every { repository.findUnsummarized(any(), isNull(), isNull()) } returns emptyList()

        val reader = UnsummarizedRepoReader(repository, pageSize = 10)

        reader.read().shouldBeNull()
        reader.read().shouldBeNull()

        verify(exactly = 1) { repository.findUnsummarized(any(), any(), any()) }
    }

    test("페이지 크기만큼 정확히 조회한다") {
        every { repository.findUnsummarized(50, null, null) } returns emptyList()

        val reader = UnsummarizedRepoReader(repository, pageSize = 50)
        reader.read()

        verify { repository.findUnsummarized(50, null, null) }
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
    readmeSummary = null,
)
