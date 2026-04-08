package com.techinsights.batch.github.community.dto

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class RedditSearchResponseTest : FunSpec({

    test("data가 null이면 top5는 빈 리스트를 반환한다") {
        val response = RedditSearchResponse(data = null)
        response.top5().shouldBeEmpty()
    }

    test("children이 없으면 top5는 빈 리스트를 반환한다") {
        val response = RedditSearchResponse(data = RedditSearchResponse.RedditData(children = emptyList()))
        response.top5().shouldBeEmpty()
    }

    test("post data가 null인 child는 제외된다") {
        val response = RedditSearchResponse(
            data = RedditSearchResponse.RedditData(
                children = listOf(
                    RedditSearchResponse.RedditChild(data = null),
                    RedditSearchResponse.RedditChild(data = buildPost("valid", score = 100)),
                )
            )
        )
        response.top5() shouldHaveSize 1
    }

    test("posts가 5개 초과면 상위 5개만 반환한다") {
        val response = RedditSearchResponse(
            data = RedditSearchResponse.RedditData(
                children = List(8) { i ->
                    RedditSearchResponse.RedditChild(data = buildPost("title$i", score = i * 10))
                }
            )
        )
        response.top5() shouldHaveSize 5
    }

    test("score 기준 내림차순으로 반환한다") {
        val response = RedditSearchResponse(
            data = RedditSearchResponse.RedditData(
                children = listOf(
                    RedditSearchResponse.RedditChild(data = buildPost("low", score = 10)),
                    RedditSearchResponse.RedditChild(data = buildPost("high", score = 800)),
                    RedditSearchResponse.RedditChild(data = buildPost("mid", score = 200)),
                )
            )
        )
        val result = response.top5()
        result[0].score shouldBe 800
        result[1].score shouldBe 200
        result[2].score shouldBe 10
    }
})

private fun buildPost(title: String, score: Int) = RedditSearchResponse.RedditPost(
    title = title,
    url = "https://reddit.com/r/test",
    score = score,
    numComments = 5,
    subreddit = "programming",
    permalink = "/r/programming/comments/abc/$title/",
    author = "testuser",
)
