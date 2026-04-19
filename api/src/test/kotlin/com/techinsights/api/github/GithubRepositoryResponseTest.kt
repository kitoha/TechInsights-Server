package com.techinsights.api.github

import com.techinsights.domain.dto.github.GithubRepositoryDto
import com.techinsights.domain.enums.CommunityStatus
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class GithubRepositoryResponseTest : FunSpec({

    fun buildDto(
        readmeSummary: String? = null,
        communityStatus: CommunityStatus? = null,
        communityMentionCount: Int? = null,
    ): GithubRepositoryDto = GithubRepositoryDto(
        id = 1L,
        repoName = "repo",
        fullName = "owner/repo",
        description = "desc",
        htmlUrl = "https://github.com/owner/repo",
        starCount = 1000L,
        forkCount = 50L,
        primaryLanguage = "Kotlin",
        ownerName = "owner",
        ownerAvatarUrl = null,
        topics = emptyList(),
        pushedAt = LocalDateTime.of(2024, 6, 1, 0, 0),
        fetchedAt = LocalDateTime.of(2024, 6, 2, 0, 0),
        weeklyStarDelta = 0L,
        dailyStarDelta = 0L,
        readmeSummary = readmeSummary,
        communityStatus = communityStatus,
        communityMentionCount = communityMentionCount,
    )

    test("fromDto - readmeSummary가 DTO 값으로 매핑된다") {
        val dto = buildDto(readmeSummary = "AI Summary Text")

        val response = GithubRepositoryResponse.fromDto(dto)

        response.readmeSummary shouldBe "AI Summary Text"
    }

    test("fromDto - communityStatus가 DTO 값으로 매핑된다") {
        val dto = buildDto(communityStatus = CommunityStatus.COMPLETED)

        val response = GithubRepositoryResponse.fromDto(dto)

        response.communityStatus shouldBe CommunityStatus.COMPLETED
    }

    test("fromDto - communityMentionCount가 DTO 값으로 매핑된다") {
        val dto = buildDto(communityMentionCount = 42)

        val response = GithubRepositoryResponse.fromDto(dto)

        response.communityMentionCount shouldBe 42
    }

    test("fromDto - community 필드가 null이면 null로 매핑된다") {
        val dto = buildDto()

        val response = GithubRepositoryResponse.fromDto(dto)

        response.communityStatus shouldBe null
        response.communityMentionCount shouldBe null
    }
})
