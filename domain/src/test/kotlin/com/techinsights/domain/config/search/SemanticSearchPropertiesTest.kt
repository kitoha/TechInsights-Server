package com.techinsights.domain.config.search

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class SemanticSearchPropertiesTest : FunSpec({

    test("기본값이 올바르게 설정된다") {
        val props = SemanticSearchProperties()

        props.defaultSize shouldBe 10
        props.maxSize shouldBe 20
    }

    test("커스텀 값으로 생성할 수 있다") {
        val props = SemanticSearchProperties(defaultSize = 5, maxSize = 15)

        props.defaultSize shouldBe 5
        props.maxSize shouldBe 15
    }

    test("defaultSize와 maxSize가 같아도 유효하다") {
        val props = SemanticSearchProperties(defaultSize = 10, maxSize = 10)

        props.defaultSize shouldBe 10
        props.maxSize shouldBe 10
    }

    test("defaultSize가 0 이하이면 예외가 발생한다") {
        val ex = shouldThrow<IllegalArgumentException> {
            SemanticSearchProperties(defaultSize = 0, maxSize = 20)
        }
        ex.message shouldContain "defaultSize"
    }

    test("defaultSize가 음수이면 예외가 발생한다") {
        shouldThrow<IllegalArgumentException> {
            SemanticSearchProperties(defaultSize = -1, maxSize = 20)
        }
    }

    test("maxSize가 defaultSize보다 작으면 예외가 발생한다") {
        val ex = shouldThrow<IllegalArgumentException> {
            SemanticSearchProperties(defaultSize = 10, maxSize = 5)
        }
        ex.message shouldContain "maxSize"
    }
})
