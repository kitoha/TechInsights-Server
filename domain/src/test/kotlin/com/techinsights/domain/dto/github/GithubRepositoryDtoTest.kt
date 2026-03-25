package com.techinsights.domain.dto.github

import com.techinsights.domain.entity.github.GithubRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class GithubRepositoryDtoTest : FunSpec({

    fun buildEntity(
        weeklyStarDelta: Long = 0L,
        dailyStarDelta: Long = 0L,
    ): GithubRepository = GithubRepository(
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
        topics = "kotlin,test",
        pushedAt = LocalDateTime.of(2024, 6, 1, 0, 0),
        fetchedAt = LocalDateTime.of(2024, 6, 2, 0, 0),
        weeklyStarDelta = weeklyStarDelta,
        dailyStarDelta = dailyStarDelta,
    )

    test("fromEntity - dailyStarDelta가 Entity 값으로 매핑된다") {
        val entity = buildEntity(dailyStarDelta = 42L)

        val dto = GithubRepositoryDto.fromEntity(entity)

        dto.dailyStarDelta shouldBe 42L
    }

    test("fromEntity - weeklyStarDelta와 dailyStarDelta가 독립적으로 매핑된다") {
        val entity = buildEntity(weeklyStarDelta = 200L, dailyStarDelta = 30L)

        val dto = GithubRepositoryDto.fromEntity(entity)

        dto.weeklyStarDelta shouldBe 200L
        dto.dailyStarDelta shouldBe 30L
    }
})
