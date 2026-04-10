package com.techinsights.batch.github.community.analyze.processor

import com.techinsights.domain.dto.github.GithubRepositoryDto
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class CommunityAnalyzeProcessorTest : FunSpec({

    val processor = CommunityAnalyzeProcessor()

    test("정상적인 communityHighlights JSON을 파싱해 CommunityAnalysisInput을 반환한다") {
        val highlights = """
            {
              "hnPosts": [
                {"platform":"hn","title":"HN Post 1","score":100,"commentCount":50,"username":"user1","url":"https://news.ycombinator.com/item?id=1"}
              ],
              "redditPosts": [
                {"platform":"reddit","title":"Reddit Post 1","score":200,"commentCount":30,"username":"redditor1","url":"https://reddit.com/r/programming/1"}
              ]
            }
        """.trimIndent()
        val item = createDto("owner/repo1", communityHighlights = highlights)

        val result = processor.process(item)!!

        result.repoFullName shouldBe "owner/repo1"
        result.repoName shouldBe "repo1"
        result.hnPosts.size shouldBe 1
        result.hnPosts[0].platform shouldBe "hn"
        result.hnPosts[0].title shouldBe "HN Post 1"
        result.hnPosts[0].score shouldBe 100
        result.redditPosts.size shouldBe 1
        result.redditPosts[0].platform shouldBe "reddit"
        result.redditPosts[0].title shouldBe "Reddit Post 1"
    }

    test("communityHighlights가 null이면 null을 반환한다") {
        val item = createDto("owner/repo1", communityHighlights = null)

        processor.process(item).shouldBeNull()
    }

    test("잘못된 JSON이면 null을 반환한다") {
        val item = createDto("owner/repo1", communityHighlights = "invalid-json")

        processor.process(item).shouldBeNull()
    }

    test("hnPosts와 redditPosts가 올바르게 분리된다") {
        val highlights = """
            {
              "hnPosts": [
                {"platform":"hn","title":"HN1","score":10,"commentCount":5,"username":"u1","url":"https://hn/1"},
                {"platform":"hn","title":"HN2","score":20,"commentCount":10,"username":"u2","url":"https://hn/2"}
              ],
              "redditPosts": [
                {"platform":"reddit","title":"R1","score":30,"commentCount":15,"username":"r1","url":"https://reddit/1"}
              ]
            }
        """.trimIndent()
        val item = createDto("owner/repo2", communityHighlights = highlights)

        val result = processor.process(item)!!

        result.hnPosts.size shouldBe 2
        result.redditPosts.size shouldBe 1
        result.hnPosts[0].title shouldBe "HN1"
        result.hnPosts[1].title shouldBe "HN2"
    }

    test("hnPosts 또는 redditPosts 필드가 없으면 빈 리스트로 처리된다") {
        val highlights = """{"hnPosts": [{"platform":"hn","title":"HN1","score":10,"commentCount":5,"username":"u1","url":"https://hn/1"}]}"""
        val item = createDto("owner/repo3", communityHighlights = highlights)

        val result = processor.process(item)!!

        result.hnPosts.size shouldBe 1
        result.redditPosts.size shouldBe 0
    }

    test("username이 null인 CommunityPost도 정상 파싱된다") {
        val highlights = """
            {
              "hnPosts": [
                {"platform":"hn","title":"HN Post","score":50,"commentCount":10,"username":null,"url":"https://hn/1"}
              ],
              "redditPosts": []
            }
        """.trimIndent()
        val item = createDto("owner/repo4", communityHighlights = highlights)

        val result = processor.process(item)!!

        result.hnPosts[0].username shouldBe null
    }
})

private fun createDto(
    fullName: String,
    communityHighlights: String? = null,
) = GithubRepositoryDto(
    id = fullName.hashCode().toLong(),
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
    communityHighlights = communityHighlights,
)
