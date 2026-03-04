package com.techinsights.batch.github.embedding.reader

import com.techinsights.batch.github.embedding.config.props.GithubEmbeddingBatchProperties
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

class UnembeddedRepoReaderTest : FunSpec({

    val repository = mockk<GithubRepositoryRepository>()
    val properties = mockk<GithubEmbeddingBatchProperties>()

    beforeTest { clearAllMocks() }

    test("첫 번째 read() 호출 시 DB에서 페이지를 로드하고 첫 항목을 반환한다") {
        val repo1 = createDto("owner/repo1", starCount = 1000L, id = 1L)
        val repo2 = createDto("owner/repo2", starCount = 900L, id = 2L)
        every { repository.findUnembedded(any(), isNull(), isNull()) } returns listOf(repo1, repo2)
        every { properties.pageSize } returns 10
        every { properties.maxItemsPerRun } returns Int.MAX_VALUE

        val reader = UnembeddedRepoReader(repository, properties, maxItemsPerRunParam = null)

        reader.read() shouldBe repo1
        reader.read() shouldBe repo2
    }

    test("버퍼가 소진되면 커서 기반으로 다음 페이지를 조회한다") {
        val repo1 = createDto("owner/repo1", starCount = 1000L, id = 1L)
        val repo2 = createDto("owner/repo2", starCount = 500L, id = 2L)

        every { repository.findUnembedded(2, null, null) } returns listOf(repo1)
        every { repository.findUnembedded(2, 1000L, 1L) } returns listOf(repo2)
        every { repository.findUnembedded(2, 500L, 2L) } returns emptyList()
        every { properties.pageSize } returns 2
        every { properties.maxItemsPerRun } returns Int.MAX_VALUE

        val reader = UnembeddedRepoReader(repository, properties, maxItemsPerRunParam = null)

        reader.read() shouldBe repo1
        reader.read() shouldBe repo2
        reader.read().shouldBeNull()
    }

    test("DB가 빈 리스트를 반환하면 null을 반환한다") {
        every { repository.findUnembedded(any(), isNull(), isNull()) } returns emptyList()
        every { properties.pageSize } returns 10
        every { properties.maxItemsPerRun } returns Int.MAX_VALUE

        val reader = UnembeddedRepoReader(repository, properties, maxItemsPerRunParam = null)

        reader.read().shouldBeNull()
    }

    test("소진 후 다시 호출해도 DB를 추가 조회하지 않는다") {
        every { repository.findUnembedded(any(), isNull(), isNull()) } returns emptyList()
        every { properties.pageSize } returns 10
        every { properties.maxItemsPerRun } returns Int.MAX_VALUE

        val reader = UnembeddedRepoReader(repository, properties, maxItemsPerRunParam = null)

        reader.read().shouldBeNull()
        reader.read().shouldBeNull()

        verify(exactly = 1) { repository.findUnembedded(any(), any(), any()) }
    }

    test("maxItemsPerRun에 도달하면 null을 반환한다") {
        val repo1 = createDto("owner/repo1", id = 1L)
        val repo2 = createDto("owner/repo2", id = 2L)
        every { repository.findUnembedded(any(), isNull(), isNull()) } returns listOf(repo1, repo2)
        every { properties.pageSize } returns 10
        every { properties.maxItemsPerRun } returns Int.MAX_VALUE

        val reader = UnembeddedRepoReader(repository, properties, maxItemsPerRunParam = 1L)

        reader.read() shouldBe repo1
        reader.read().shouldBeNull()
    }

    test("페이지 크기만큼 정확히 조회한다") {
        every { repository.findUnembedded(50, null, null) } returns emptyList()
        every { properties.pageSize } returns 50
        every { properties.maxItemsPerRun } returns Int.MAX_VALUE

        val reader = UnembeddedRepoReader(repository, properties, maxItemsPerRunParam = null)
        reader.read()

        verify { repository.findUnembedded(50, null, null) }
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
    readmeSummary = "summary",
)
