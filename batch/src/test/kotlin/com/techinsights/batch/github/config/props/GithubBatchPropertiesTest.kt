package com.techinsights.batch.github.config.props

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldContain
import java.time.LocalDate

class GithubBatchPropertiesTest : FunSpec({

    test("동적 연도 기반으로 3개 쿼리가 생성된다") {
        val props = GithubBatchProperties()
        val year = LocalDate.now().year

        props.queries shouldHaveSize 3
        props.queries[0].query shouldContain "${year - 2}"
        props.queries[1].query shouldContain "${year - 1}"
        props.queries[2].query shouldContain "$year"
    }

    test("모든 쿼리에 stars:>100 조건이 포함된다") {
        val props = GithubBatchProperties()

        props.queries.forEach { queryConfig ->
            queryConfig.query shouldContain "stars:>100"
        }
    }

    test("매 호출마다 현재 연도 기준으로 생성된다") {
        val props = GithubBatchProperties()
        val year = LocalDate.now().year

        val firstCall = props.queries
        val secondCall = props.queries

        firstCall[0].query shouldContain "${year - 2}"
        secondCall[0].query shouldContain "${year - 2}"
    }
})
