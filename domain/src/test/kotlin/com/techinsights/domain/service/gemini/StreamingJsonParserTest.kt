package com.techinsights.domain.service.gemini

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class StreamingJsonParserTest : FunSpec({
    lateinit var parser: StreamingJsonParser

    beforeTest {
        parser = StreamingJsonParser()
    }

    test("should parse a single complete JSON object") {
        val chunk = "{\"id\": \"1\", \"success\": true, \"summary\": \"test\"}"
        val result = parser.process(chunk)
        
        result shouldHaveSize 1
        result[0].id shouldBe "1"
        result[0].success shouldBe true
    }

    test("should parse multiple JSON objects in one chunk") {
        val chunk = "{\"id\": \"1\", \"success\": true}{\"id\": \"2\", \"success\": true}"
        val result = parser.process(chunk)
        
        result shouldHaveSize 2
        result[0].id shouldBe "1"
        result[1].id shouldBe "2"
    }

    test("should parse fragmented JSON objects across multiple chunks") {
        val chunk1 = "{\"id\": \"1\", \"suc"
        val chunk2 = "cess\": true, \"summary\": \"part1 "
        val chunk3 = "part2\"}" 
        
        parser.process(chunk1) shouldHaveSize 0
        parser.process(chunk2) shouldHaveSize 0
        val result = parser.process(chunk3)
        
        result shouldHaveSize 1
        result[0].id shouldBe "1"
        result[0].summary shouldBe "part1 part2"
    }

    test("should ignore wrapper object and extract nested objects") {
        val chunk = "{\"results\": [{\"id\": \"1\", \"success\": true}, {\"id\": \"2\", \"success\": true}]}" 
        val result = parser.process(chunk)
        
        result shouldHaveSize 2
        result[0].id shouldBe "1"
        result[1].id shouldBe "2"
    }

    test("should handle nested braces inside string values") {
        val chunk = "{\"id\": \"1\", \"summary\": \"nested {braces} here\"}"
        val result = parser.process(chunk)
        
        result shouldHaveSize 1
        result[0].summary shouldBe "nested {braces} here"
    }

    test("should handle escaped quotes inside string values") {
        val chunk = "{\"id\": \"1\", \"summary\": \"escaped \\\"quote\\\" here\"}"
        val result = parser.process(chunk)
        
        result shouldHaveSize 1
        result[0].summary shouldBe "escaped \"quote\" here"
    }

    test("should skip objects with missing required ID field") {
        val chunk = "{\"no_id\": \"none\"}{\"id\": \"valid\"}"
        val result = parser.process(chunk)
        
        result shouldHaveSize 1
        result[0].id shouldBe "valid"
    }

    test("should handle noise between objects") {
        val chunk = " , [ { \"id\": \"1\" } , { \"id\": \"2\" } ] "
        val result = parser.process(chunk)
        
        result shouldHaveSize 2
        result[0].id shouldBe "1"
        result[1].id shouldBe "2"
    }
})
