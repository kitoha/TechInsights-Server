package com.techinsights.batch.github.community.dto

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class HnSearchResponseTest : FunSpec({

    test("hits가 없으면 top5는 빈 리스트를 반환한다") {
        val response = HnSearchResponse(hits = emptyList())
        response.top5().shouldBeEmpty()
    }

    test("hits가 5개 이하면 전부 반환한다") {
        val response = HnSearchResponse(hits = listOf(
            HnSearchResponse.HnHit("1", "title1", "url1", points = 100),
            HnSearchResponse.HnHit("2", "title2", "url2", points = 50),
        ))
        response.top5() shouldHaveSize 2
    }

    test("hits가 5개 초과면 상위 5개만 반환한다") {
        val response = HnSearchResponse(hits = List(10) { i ->
            HnSearchResponse.HnHit("$i", "title$i", "url$i", points = i * 10)
        })
        response.top5() shouldHaveSize 5
    }

    test("points 기준 내림차순으로 반환한다") {
        val response = HnSearchResponse(hits = listOf(
            HnSearchResponse.HnHit("1", "low", "url1", points = 10),
            HnSearchResponse.HnHit("2", "high", "url2", points = 500),
            HnSearchResponse.HnHit("3", "mid", "url3", points = 100),
        ))
        val result = response.top5()
        result[0].points shouldBe 500
        result[1].points shouldBe 100
        result[2].points shouldBe 10
    }
})
