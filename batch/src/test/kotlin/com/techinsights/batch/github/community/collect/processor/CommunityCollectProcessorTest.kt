package com.techinsights.batch.github.community.collect.processor

import com.techinsights.batch.github.community.dto.CommunityBuzzInput
import com.techinsights.batch.github.community.service.CommunityFetcher
import com.techinsights.domain.dto.community.CommunityPost
import com.techinsights.domain.dto.github.GithubRepositoryDto
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.LocalDateTime

class CommunityCollectProcessorTest : FunSpec({

    val fetcher = mockk<CommunityFetcher>()

    beforeTest { clearAllMocks() }

    test("CommunityFetcher를 호출해 CommunityBuzzInput을 반환한다") {
        val hnPost = CommunityPost("hn", "HN Title", 100, 50, "user1", "https://hn/1")
        val expected = CommunityBuzzInput(
            repoFullName = "owner/repo",
            ownerName = "owner",
            repoName = "repo",
            hnPosts = listOf(hnPost),
            redditPosts = emptyList(),
            prevMentionCount = null,
            updateCount = 0,
        )
        coEvery {
            fetcher.fetch(
                repoFullName = "owner/repo",
                ownerName = "owner",
                repoName = "repo",
                prevMentionCount = null,
                updateCount = 0,
            )
        } returns expected

        val processor = CommunityCollectProcessor(fetcher)
        val result = processor.process(createDto("owner/repo"))

        result shouldBe expected
    }

    test("communityMentionCount를 prevMentionCount로 전달한다") {
        val dto = createDto("owner/repo2", communityMentionCount = 42, communityUpdateCount = 3)
        val expected = CommunityBuzzInput(
            repoFullName = "owner/repo2",
            ownerName = "owner",
            repoName = "repo2",
            hnPosts = emptyList(),
            redditPosts = emptyList(),
            prevMentionCount = 42,
            updateCount = 3,
        )
        coEvery {
            fetcher.fetch(
                repoFullName = "owner/repo2",
                ownerName = "owner",
                repoName = "repo2",
                prevMentionCount = 42,
                updateCount = 3,
            )
        } returns expected

        val processor = CommunityCollectProcessor(fetcher)
        val result = processor.process(dto)

        result shouldBe expected
        coVerify {
            fetcher.fetch(
                repoFullName = "owner/repo2",
                ownerName = "owner",
                repoName = "repo2",
                prevMentionCount = 42,
                updateCount = 3,
            )
        }
    }

    test("communityMentionCount가 null이면 prevMentionCount로 null을 전달한다") {
        val dto = createDto("owner/repo3", communityMentionCount = null)
        val expected = CommunityBuzzInput(
            repoFullName = "owner/repo3",
            ownerName = "owner",
            repoName = "repo3",
            hnPosts = emptyList(),
            redditPosts = emptyList(),
            prevMentionCount = null,
            updateCount = 0,
        )
        coEvery {
            fetcher.fetch(
                repoFullName = "owner/repo3",
                ownerName = "owner",
                repoName = "repo3",
                prevMentionCount = null,
                updateCount = 0,
            )
        } returns expected

        val processor = CommunityCollectProcessor(fetcher)
        processor.process(dto) shouldBe expected
    }
})

private fun createDto(
    fullName: String,
    id: Long = fullName.hashCode().toLong(),
    communityMentionCount: Int? = null,
    communityUpdateCount: Int = 0,
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
    communityMentionCount = communityMentionCount,
    communityUpdateCount = communityUpdateCount,
)
