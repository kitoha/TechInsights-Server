package com.techinsights.domain.repository.search.query

import com.techinsights.domain.config.search.ScoreWeights
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlin.math.ln

class SearchQueryBuilderTest : FunSpec({

    val weights = ScoreWeights()
    val queryBuilder = SearchQueryBuilder(weights)

    fun popularity(viewCount: Long): Double = ln(1.0 + viewCount) * weights.viewCountMultiplier

    test("인기도 점수는 view_count에 선형이 아니라 로그로 증가한다") {
        val expression = queryBuilder.buildRelevanceScore("kotlin").toString()

        expression shouldContain "ln(1.0 + "
        expression shouldNotContain "post.viewCount * "
    }

    test("제목이 일치하는 조회수 0 게시글이 본문만 일치하는 조회수 1천만 게시글보다 높다") {
        val titleMatchNoViews = weights.titleContains + popularity(0)
        val contentMatchViral = weights.content + popularity(10_000_000)

        (titleMatchNoViews > contentMatchViral) shouldBe true
    }

    test("같은 텍스트 점수라면 조회수가 많은 게시글이 높다") {
        (popularity(1_000) > popularity(10)) shouldBe true
    }

    test("회사명 일치는 조인 LIKE가 아니라 일치한 회사 id 목록으로 매칭한다") {
        val condition = queryBuilder.buildSearchCondition("toss", null, listOf(3L, 7L)).toString()

        condition shouldContain "post.company.id in [3, 7]"
        condition shouldNotContain "company.name"
    }

    test("일치한 회사가 없으면 회사 조건 없이 게시글 컬럼 조건만 사용한다") {
        val condition = queryBuilder.buildSearchCondition("toss", null, emptyList()).toString()

        condition shouldNotContain "post.company.id"
        condition shouldContain "containsIc(post.title"
    }

    test("회사명 일치 점수는 기존처럼 유지된다") {
        queryBuilder.buildRelevanceScore("toss").toString() shouldContain "company.name"
    }
})
