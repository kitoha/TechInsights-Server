package com.techinsights.domain.config.search

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class SemanticSearchPropertiesTest : FunSpec({

    test("기본값이 올바르게 설정된다") {
        val props = SemanticSearchProperties()

        props.defaultSize shouldBe 10
        props.maxSize shouldBe 20
    }

    test("커스텀 값으로 생성할 수 있다") {
        val props = SemanticSearchProperties(
            defaultSize = 5,
            maxSize = 15
        )

        props.defaultSize shouldBe 5
        props.maxSize shouldBe 15
    }

    test("defaultSize는 양수여야 한다") {
        val props = SemanticSearchProperties(defaultSize = 1)

        props.defaultSize shouldNotBe 0
    }

    test("maxSize는 defaultSize보다 크거나 같아야 한다") {
        val props = SemanticSearchProperties(defaultSize = 10, maxSize = 20)

        (props.maxSize >= props.defaultSize) shouldBe true
    }
})
