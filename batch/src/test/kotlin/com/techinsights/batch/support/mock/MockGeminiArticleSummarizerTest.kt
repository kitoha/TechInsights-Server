package com.techinsights.batch.support.mock

import com.techinsights.domain.enums.GeminiModelType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith

class MockGeminiArticleSummarizerTest : FunSpec({

    val mockSummarizer = MockGeminiArticleSummarizer()

    test("summarize should return valid SummaryResult") {
        // given
        val article = "This is a test article about Spring Boot and Kotlin development."
        val modelType = GeminiModelType.GEMINI_2_5_FLASH_LITE

        // when
        val result = mockSummarizer.summarize(article, modelType)

        // then
        result shouldNotBe null
        result.summary shouldNotBe null
        result.preview shouldNotBe null
        result.categories.isNotEmpty() shouldBe true
    }

    test("summarize should include MOCK_SUMMARY prefix in summary") {
        // given
        val article = "Test article content"
        val modelType = GeminiModelType.GEMINI_2_5_FLASH_LITE

        // when
        val result = mockSummarizer.summarize(article, modelType)

        // then
        result.summary shouldStartWith "MOCK_SUMMARY:"
    }

    test("summarize should include MOCK_PREVIEW prefix in preview") {
        // given
        val article = "Another test article"
        val modelType = GeminiModelType.GEMINI_2_5_FLASH_LITE

        // when
        val result = mockSummarizer.summarize(article, modelType)

        // then
        result.preview shouldStartWith "MOCK_PREVIEW:"
    }

    test("summarize should truncate article to 100 characters in preview") {
        // given
        val longArticle = "A".repeat(200)
        val modelType = GeminiModelType.GEMINI_2_5_FLASH_LITE

        // when
        val result = mockSummarizer.summarize(longArticle, modelType)

        // then
        result.preview shouldContain "A".repeat(100)
        result.preview?.length shouldBe ("MOCK_PREVIEW: " + "A".repeat(100)).length
    }

    test("summarize should return one of the predefined categories") {
        // given
        val article = "Test article about backend development"
        val modelType = GeminiModelType.GEMINI_2_5_FLASH_LITE
        val validCategories = listOf("FrontEnd", "BackEnd", "AI", "BigData", "Infra", "Architecture")

        // when
        val result = mockSummarizer.summarize(article, modelType)

        // then
        result.categories.size shouldBe 1
        validCategories.contains(result.categories[0]) shouldBe true
    }

    test("summarize should include category in summary text") {
        // given
        val article = "Article about microservices"
        val modelType = GeminiModelType.GEMINI_2_5_FLASH_LITE

        // when
        val result = mockSummarizer.summarize(article, modelType)

        // then
        result.summary shouldContain result.categories[0]
        result.summary shouldContain "기술 블로그 포스트입니다"
    }

    test("summarize should handle short articles") {
        // given
        val shortArticle = "Short"
        val modelType = GeminiModelType.GEMINI_2_5_FLASH_LITE

        // when
        val result = mockSummarizer.summarize(shortArticle, modelType)

        // then
        result.preview shouldContain shortArticle
        result.summary shouldNotBe null
    }

    test("summarize should replace newlines with spaces in preview") {
        // given
        val articleWithNewlines = "Line 1\nLine 2\nLine 3\nLine 4"
        val modelType = GeminiModelType.GEMINI_2_5_FLASH_LITE

        // when
        val result = mockSummarizer.summarize(articleWithNewlines, modelType)

        // then
        result.preview shouldContain " "
    }

    test("summarize should work with different model types") {
        // given
        val article = "Test article for different models"

        // when
        val result1 = mockSummarizer.summarize(article, GeminiModelType.GEMINI_2_5_FLASH_LITE)
        val result2 = mockSummarizer.summarize(article, GeminiModelType.GEMINI_2_5_FLASH)
        val result3 = mockSummarizer.summarize(article, GeminiModelType.GEMINI_EMBEDDING)

        // then
        result1 shouldNotBe null
        result2 shouldNotBe null
        result3 shouldNotBe null
        result1.summary shouldStartWith "MOCK_SUMMARY:"
        result2.summary shouldStartWith "MOCK_SUMMARY:"
        result3.summary shouldStartWith "MOCK_SUMMARY:"
    }

    test("summarize should handle empty article") {
        // given
        val emptyArticle = ""
        val modelType = GeminiModelType.GEMINI_2_5_FLASH_LITE

        // when
        val result = mockSummarizer.summarize(emptyArticle, modelType)

        // then
        result.summary shouldNotBe null
        result.preview shouldBe "MOCK_PREVIEW: "
        result.categories.isNotEmpty() shouldBe true
    }

    test("summarize should handle very long articles") {
        // given
        val longArticle = "This is a very long article. ".repeat(1000)
        val modelType = GeminiModelType.GEMINI_2_5_FLASH_LITE

        // when
        val result = mockSummarizer.summarize(longArticle, modelType)

        // then
        result shouldNotBe null
        result.preview?.length shouldBe ("MOCK_PREVIEW: " + longArticle.take(100)).length
    }

    test("summarize should handle special characters") {
        // given
        val articleWithSpecialChars = "Article with special chars: !@#$%^&*()[]{}|<>?/~`"
        val modelType = GeminiModelType.GEMINI_2_5_FLASH_LITE

        // when
        val result = mockSummarizer.summarize(articleWithSpecialChars, modelType)

        // then
        result shouldNotBe null
        result.preview shouldContain articleWithSpecialChars.take(100)
    }

    test("summarize should handle Korean text") {
        // given
        val koreanArticle = "한글로 작성된 기술 블로그 포스트입니다. 스프링 부트와 코틀린에 대한 내용을 다룹니다."
        val modelType = GeminiModelType.GEMINI_2_5_FLASH_LITE

        // when
        val result = mockSummarizer.summarize(koreanArticle, modelType)

        // then
        result shouldNotBe null
        result.preview shouldContain koreanArticle.take(100)
        result.summary shouldContain "기술 블로그 포스트입니다"
    }
})
