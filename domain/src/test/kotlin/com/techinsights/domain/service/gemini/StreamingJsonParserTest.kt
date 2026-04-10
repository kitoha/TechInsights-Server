package com.techinsights.domain.service.gemini

import com.techinsights.domain.dto.community.CommunityAnalysisResult
import com.techinsights.domain.dto.gemini.SummaryResultWithId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class StreamingJsonParserTest : FunSpec({

    context("SummaryResultWithId 파싱") {
        lateinit var parser: StreamingJsonParser<SummaryResultWithId>

        beforeTest {
            parser = StreamingJsonParser(SummaryResultWithId::class.java) { it.id }
        }

        test("단일 완성 JSON 객체를 파싱한다") {
            val chunk = """{"id": "1", "success": true, "summary": "test"}"""
            val result = parser.process(chunk)

            result shouldHaveSize 1
            result[0].id shouldBe "1"
            result[0].success shouldBe true
        }

        test("한 청크에 여러 JSON 객체를 파싱한다") {
            val chunk = """{"id": "1", "success": true}{"id": "2", "success": true}"""
            val result = parser.process(chunk)

            result shouldHaveSize 2
            result[0].id shouldBe "1"
            result[1].id shouldBe "2"
        }

        test("여러 청크에 걸쳐 분할된 JSON을 파싱한다") {
            val chunk1 = """{"id": "1", "suc"""
            val chunk2 = """cess": true, "summary": "part1 """
            val chunk3 = """part2"}"""

            parser.process(chunk1) shouldHaveSize 0
            parser.process(chunk2) shouldHaveSize 0
            val result = parser.process(chunk3)

            result shouldHaveSize 1
            result[0].id shouldBe "1"
            result[0].summary shouldBe "part1 part2"
        }

        test("wrapper 객체를 무시하고 내부 객체를 추출한다") {
            val chunk = """{"results": [{"id": "1", "success": true}, {"id": "2", "success": true}]}"""
            val result = parser.process(chunk)

            result shouldHaveSize 2
            result[0].id shouldBe "1"
            result[1].id shouldBe "2"
        }

        test("문자열 값 안의 중괄호를 올바르게 처리한다") {
            val chunk = """{"id": "1", "summary": "nested {braces} here"}"""
            val result = parser.process(chunk)

            result shouldHaveSize 1
            result[0].summary shouldBe "nested {braces} here"
        }

        test("문자열 값 안의 이스케이프된 따옴표를 처리한다") {
            val chunk = """{"id": "1", "summary": "escaped \"quote\" here"}"""
            val result = parser.process(chunk)

            result shouldHaveSize 1
            result[0].summary shouldBe """escaped "quote" here"""
        }

        test("id 필드가 없는 객체는 건너뛴다") {
            val chunk = """{"no_id": "none"}{"id": "valid"}"""
            val result = parser.process(chunk)

            result shouldHaveSize 1
            result[0].id shouldBe "valid"
        }

        test("객체 사이의 노이즈를 무시한다") {
            val chunk = """ , [ { "id": "1" } , { "id": "2" } ] """
            val result = parser.process(chunk)

            result shouldHaveSize 2
            result[0].id shouldBe "1"
            result[1].id shouldBe "2"
        }
    }

    context("CommunityAnalysisResult 파싱") {
        lateinit var parser: StreamingJsonParser<CommunityAnalysisResult>

        beforeTest {
            parser = StreamingJsonParser(CommunityAnalysisResult::class.java) { it.id }
        }

        test("성공한 커뮤니티 분석 결과를 파싱한다") {
            val chunk = """{"id": "owner/repo", "success": true, "sentiment": {"positive": 70, "neutral": 20, "negative": 10}, "insights": [{"text": "좋다", "tone": "positive"}]}"""
            val result = parser.process(chunk)

            result shouldHaveSize 1
            result[0].id shouldBe "owner/repo"
            result[0].success shouldBe true
            result[0].sentiment?.positive shouldBe 70
            result[0].insights?.first()?.text shouldBe "좋다"
        }

        test("실패한 커뮤니티 분석 결과를 파싱한다") {
            val chunk = """{"id": "owner/repo", "success": false, "error": "API timeout"}"""
            val result = parser.process(chunk)

            result shouldHaveSize 1
            result[0].id shouldBe "owner/repo"
            result[0].success shouldBe false
            result[0].error shouldBe "API timeout"
        }

        test("여러 청크에 걸쳐 분할된 커뮤니티 결과를 파싱한다") {
            val chunk1 = """{"id": "owner/re"""
            val chunk2 = """po", "success": true}"""

            parser.process(chunk1) shouldHaveSize 0
            val result = parser.process(chunk2)

            result shouldHaveSize 1
            result[0].id shouldBe "owner/repo"
        }

        test("id 필드가 없는 커뮤니티 결과는 건너뛴다") {
            val chunk = """{"success": true}{"id": "owner/repo", "success": false}"""
            val result = parser.process(chunk)

            result shouldHaveSize 1
            result[0].id shouldBe "owner/repo"
        }

        test("wrapper 배열 안의 여러 커뮤니티 결과를 추출한다") {
            val chunk = """{"results": [{"id": "a/b", "success": true}, {"id": "c/d", "success": false}]}"""
            val result = parser.process(chunk)

            result shouldHaveSize 2
            result[0].id shouldBe "a/b"
            result[1].id shouldBe "c/d"
        }
    }
})
