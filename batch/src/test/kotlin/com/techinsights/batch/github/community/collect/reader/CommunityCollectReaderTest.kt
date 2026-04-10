package com.techinsights.batch.github.community.collect.reader

import com.techinsights.batch.github.community.collect.config.props.CommunityCollectBatchProperties
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

class CommunityCollectReaderTest : FunSpec({

    val repository = mockk<GithubRepositoryRepository>()
    val properties = mockk<CommunityCollectBatchProperties>()

    beforeTest { clearAllMocks() }

    fun defaultProperties(
        pageSize: Int = 10,
        maxItemsPerRun: Int = Int.MAX_VALUE,
        normalRefreshDays: Long = 14L,
        noMentionsRefreshDays: Long = 30L,
    ) {
        every { properties.pageSize } returns pageSize
        every { properties.maxItemsPerRun } returns maxItemsPerRun
        every { properties.normalRefreshDays } returns normalRefreshDays
        every { properties.noMentionsRefreshDays } returns noMentionsRefreshDays
    }

    test("read() 첫 호출 시 DB를 조회하고 첫 항목을 반환한다") {
        val repo1 = createDto("owner/repo1", id = 1L)
        val repo2 = createDto("owner/repo2", id = 2L)
        defaultProperties()
        every {
            repository.findForCommunityCollect(any(), isNull(), isNull(), any(), any())
        } returns listOf(repo1, repo2)

        val reader = CommunityCollectReader(repository, properties, maxItemsPerRunParam = null)

        reader.read() shouldBe repo1
        reader.read() shouldBe repo2
    }

    test("버퍼 소진 시 다음 페이지를 커서 기반으로 조회한다") {
        val collectTime = LocalDateTime.of(2026, 1, 1, 0, 0)
        val repo1 = createDto("owner/repo1", id = 1L, communityCollectedAt = collectTime)
        val repo2 = createDto("owner/repo2", id = 2L, communityCollectedAt = null)

        defaultProperties(pageSize = 1)
        every {
            repository.findForCommunityCollect(1, null, null, any(), any())
        } returns listOf(repo1)
        every {
            repository.findForCommunityCollect(1, collectTime, 1L, any(), any())
        } returns listOf(repo2)
        every {
            repository.findForCommunityCollect(1, null, 2L, any(), any())
        } returns emptyList()

        val reader = CommunityCollectReader(repository, properties, maxItemsPerRunParam = null)

        reader.read() shouldBe repo1
        reader.read() shouldBe repo2
        reader.read().shouldBeNull()
    }

    test("maxItemsPerRun 도달 시 null을 반환한다") {
        val repo1 = createDto("owner/repo1", id = 1L)
        val repo2 = createDto("owner/repo2", id = 2L)
        defaultProperties(maxItemsPerRun = 1)
        every {
            repository.findForCommunityCollect(any(), isNull(), isNull(), any(), any())
        } returns listOf(repo1, repo2)

        val reader = CommunityCollectReader(repository, properties, maxItemsPerRunParam = null)

        reader.read() shouldBe repo1
        reader.read().shouldBeNull()
    }

    test("DB 결과가 비면 null을 반환한다 (exhausted)") {
        defaultProperties()
        every {
            repository.findForCommunityCollect(any(), isNull(), isNull(), any(), any())
        } returns emptyList()

        val reader = CommunityCollectReader(repository, properties, maxItemsPerRunParam = null)

        reader.read().shouldBeNull()
    }

    test("exhausted 이후 read() 재호출해도 DB를 추가 조회하지 않는다") {
        defaultProperties()
        every {
            repository.findForCommunityCollect(any(), isNull(), isNull(), any(), any())
        } returns emptyList()

        val reader = CommunityCollectReader(repository, properties, maxItemsPerRunParam = null)

        reader.read().shouldBeNull()
        reader.read().shouldBeNull()

        verify(exactly = 1) { repository.findForCommunityCollect(any(), any(), any(), any(), any()) }
    }

    test("커서 업데이트: communityCollectedAt이 null이면 lastCollectedAt은 null로 유지된다") {
        val repo1 = createDto("owner/repo1", id = 1L, communityCollectedAt = null)
        val repo2 = createDto("owner/repo2", id = 2L, communityCollectedAt = null)

        defaultProperties(pageSize = 1)
        every {
            repository.findForCommunityCollect(1, null, null, any(), any())
        } returns listOf(repo1)
        every {
            // afterCollectedAt=null, afterId=1L (null 유지, id 업데이트)
            repository.findForCommunityCollect(1, null, 1L, any(), any())
        } returns listOf(repo2)
        every {
            repository.findForCommunityCollect(1, null, 2L, any(), any())
        } returns emptyList()

        val reader = CommunityCollectReader(repository, properties, maxItemsPerRunParam = null)

        reader.read() shouldBe repo1
        reader.read() shouldBe repo2
        reader.read().shouldBeNull()

        verify { repository.findForCommunityCollect(1, null, 1L, any(), any()) }
    }

    test("jobParameters maxItemsPerRun이 properties 값을 덮어쓴다") {
        val repo1 = createDto("owner/repo1", id = 1L)
        defaultProperties(maxItemsPerRun = 9999)
        every {
            repository.findForCommunityCollect(any(), isNull(), isNull(), any(), any())
        } returns listOf(repo1)

        val reader = CommunityCollectReader(repository, properties, maxItemsPerRunParam = 1L)

        reader.read() shouldBe repo1
        reader.read().shouldBeNull()
    }
})

private fun createDto(
    fullName: String,
    id: Long = fullName.hashCode().toLong(),
    communityCollectedAt: LocalDateTime? = null,
) = GithubRepositoryDto(
    id = id,
    repoName = fullName.substringAfter("/"),
    fullName = fullName,
    description = null,
    htmlUrl = "https://github.com/$fullName",
    starCount = 1000L,
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
    communityCollectedAt = communityCollectedAt,
)
