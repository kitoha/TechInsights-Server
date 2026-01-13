package com.techinsights.domain.utils

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

class TokenEstimatorTest : FunSpec({

  test("should estimate tokens for Korean text correctly") {
    val koreanText = "안녕하세요"
    val tokens = TokenEstimator.estimateTokens(koreanText)

    // 한글 5자 * 3 = 15 토큰
    tokens shouldBe 15
  }

  test("should estimate tokens for English text correctly") {
    val englishText = "Hello World"
    val tokens = TokenEstimator.estimateTokens(englishText)

    // 영문 11자 / 2 = 5 토큰 (정수 나눗셈)
    tokens shouldBe 5
  }

  test("should estimate tokens for mixed Korean and English text") {
    val mixedText = "Hello 안녕"
    val tokens = TokenEstimator.estimateTokens(mixedText)

    // 영문 6자 / 2 = 3, 한글 2자 * 3 = 6, 총 9 토큰
    tokens shouldBe 9
  }

  test("should estimate input tokens with base prompt") {
    val content = "테스트"
    val tokens = TokenEstimator.estimateInputTokens(content)

    // 기본 프롬프트 500 + (한글 3자 * 3 = 9) = 509
    tokens shouldBe 509
  }

  test("should estimate output tokens as constant") {
    val tokens = TokenEstimator.estimateOutputTokens()

    tokens shouldBe 1000
  }

  test("should estimate total tokens correctly") {
    val content = "Hello"
    val tokens = TokenEstimator.estimateTotalTokens(content)

    // 입력: 500 + (5 / 2 = 2) = 502
    // 출력: 1000
    // 총합: 1502
    tokens shouldBe 1502
  }

  test("should handle empty string") {
    val tokens = TokenEstimator.estimateTokens("")

    tokens shouldBe 0
  }

  test("should estimate tokens for long text") {
    val longText = "a".repeat(1000) + "가".repeat(100)
    val tokens = TokenEstimator.estimateTokens(longText)

    // 영문 1000자 / 2 = 500, 한글 100자 * 3 = 300, 총 800
    tokens shouldBe 800
  }

  test("should handle special characters") {
    val specialText = "!@#$%^&*()"
    val tokens = TokenEstimator.estimateTokens(specialText)

    // 특수문자 10자 / 2 = 5
    tokens shouldBe 5
  }

  test("should estimate tokens for realistic article content") {
    val article = """
      최근 AI 기술의 발전으로 인해 많은 변화가 일어나고 있습니다.
      This is a sample article with mixed content.
    """.trimIndent()

    val tokens = TokenEstimator.estimateTokens(article)

    // 토큰 수가 0보다 크면 됨
    tokens shouldBeGreaterThan 0
  }
})
