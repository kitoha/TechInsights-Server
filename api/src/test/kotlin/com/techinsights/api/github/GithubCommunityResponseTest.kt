package com.techinsights.api.github

import com.techinsights.domain.dto.github.GithubRepositoryDto
import com.techinsights.domain.enums.CommunityStatus
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class GithubCommunityResponseTest : FunSpec({

    fun buildDto(
        communityStatus: CommunityStatus? = null,
        communitySentiment: String? = null,
        communityInsights: String? = null,
        communityHighlights: String? = null,
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
        readmeSummary = null,
        communityStatus = communityStatus,
        communitySentiment = communitySentiment,
        communityInsights = communityInsights,
        communityHighlights = communityHighlights,
        communityMentionCount = communityMentionCount,
    )

    test("fromDto - communitySentiment가 DTO 값으로 매핑된다") {
        val sentiment = """{"positive":0.8,"neutral":0.1,"negative":0.1}"""
        val dto = buildDto(communitySentiment = sentiment)

        val response = GithubCommunityResponse.fromDto(dto)

        response.communitySentiment shouldBe sentiment
    }

    test("fromDto - communityInsights가 DTO 값으로 매핑된다") {
        val insights = """[{"title":"Popular","description":"High activity"}]"""
        val dto = buildDto(communityInsights = insights)

        val response = GithubCommunityResponse.fromDto(dto)

        response.communityInsights shouldBe insights
    }

    test("fromDto - communityHighlights가 DTO 값으로 매핑된다") {
        val highlights = """{"hnPosts":[],"redditPosts":[]}"""
        val dto = buildDto(communityHighlights = highlights)

        val response = GithubCommunityResponse.fromDto(dto)

        response.communityHighlights shouldBe highlights
    }

    test("fromDto - communityStatus가 DTO 값으로 매핑된다") {
        val dto = buildDto(communityStatus = CommunityStatus.COMPLETED)

        val response = GithubCommunityResponse.fromDto(dto)

        response.communityStatus shouldBe CommunityStatus.COMPLETED
    }

    test("fromDto - communityMentionCount가 DTO 값으로 매핑된다") {
        val dto = buildDto(communityMentionCount = 42)

        val response = GithubCommunityResponse.fromDto(dto)

        response.communityMentionCount shouldBe 42
    }

    test("fromDto - 모든 필드가 null이면 null로 매핑된다") {
        val dto = buildDto()

        val response = GithubCommunityResponse.fromDto(dto)

        response.communityStatus shouldBe null
        response.communitySentiment shouldBe null
        response.communityInsights shouldBe null
        response.communityHighlights shouldBe null
        response.communityMentionCount shouldBe null
    }
})
