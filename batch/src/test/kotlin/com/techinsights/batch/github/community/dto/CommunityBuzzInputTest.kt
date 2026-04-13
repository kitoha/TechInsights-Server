package com.techinsights.batch.github.community.dto

import com.techinsights.domain.dto.community.CommunityPost
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

class CommunityBuzzInputTest : FunSpec({

    fun buildPost(platform: String = "HackerNews") = CommunityPost(
        platform = platform,
        title = "Test post",
        score = 100,
        commentCount = 10,
        username = "user1",
        url = "https://example.com/1",
    )

    test("totalMentions은 HN + Reddit 게시글 수의 합이다") {
        val input = CommunityBuzzInput(
            repoFullName = "owner/repo",
            ownerName = "owner",
            repoName = "repo",
            hnPosts = listOf(buildPost("HackerNews"), buildPost("HackerNews")),
            redditPosts = listOf(buildPost("Reddit")),
            prevMentionCount = null,
            updateCount = 0,
        )
        input.totalMentions shouldBe 3
    }

    test("prevMentionCount가 null이면 hasNewMentions는 true다") {
        val input = CommunityBuzzInput(
            repoFullName = "owner/repo",
            ownerName = "owner",
            repoName = "repo",
            hnPosts = emptyList(),
            redditPosts = emptyList(),
            prevMentionCount = null,
            updateCount = 0,
        )
        input.hasNewMentions().shouldBeTrue()
    }

    test("현재 totalMentions가 prevMentionCount보다 크면 hasNewMentions는 true다") {
        val input = CommunityBuzzInput(
            repoFullName = "owner/repo",
            ownerName = "owner",
            repoName = "repo",
            hnPosts = listOf(buildPost()),
            redditPosts = listOf(buildPost("Reddit")),
            prevMentionCount = 1,
            updateCount = 0,
        )
        input.hasNewMentions().shouldBeTrue()
    }

    test("현재 totalMentions가 prevMentionCount와 같으면 hasNewMentions는 false다") {
        val input = CommunityBuzzInput(
            repoFullName = "owner/repo",
            ownerName = "owner",
            repoName = "repo",
            hnPosts = listOf(buildPost()),
            redditPosts = emptyList(),
            prevMentionCount = 1,
            updateCount = 0,
        )
        input.hasNewMentions().shouldBeFalse()
    }
})
