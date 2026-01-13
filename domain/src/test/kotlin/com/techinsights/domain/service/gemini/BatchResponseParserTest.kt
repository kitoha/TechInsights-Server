package com.techinsights.domain.service.gemini

import com.techinsights.domain.dto.gemini.ArticleInput
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class BatchResponseParserTest : FunSpec({

  val parser = BatchResponseParser()

  test("should parse valid JSON response") {
    val responseText = """
      {
        "results": [
          {
            "id": "1",
            "success": true,
            "summary": "Test summary",
            "categories": ["AI", "Backend"],
            "preview": "Test preview"
          }
        ]
      }
    """.trimIndent()
    val articles = listOf(ArticleInput("1", "Title", "Content"))

    val response = parser.parse(responseText, articles)

    response.results shouldHaveSize 1
    response.results[0].id shouldBe "1"
    response.results[0].success shouldBe true
    response.results[0].summary shouldBe "Test summary"
    response.results[0].categories shouldBe listOf("AI", "Backend")
    response.results[0].preview shouldBe "Test preview"
  }

  test("should parse response with multiple results") {
    val responseText = """
      {
        "results": [
          {
            "id": "1",
            "success": true,
            "summary": "Summary 1",
            "categories": ["AI"],
            "preview": "Preview 1"
          },
          {
            "id": "2",
            "success": true,
            "summary": "Summary 2",
            "categories": ["Backend"],
            "preview": "Preview 2"
          }
        ]
      }
    """.trimIndent()
    val articles = listOf(
      ArticleInput("1", "Title 1", "Content 1"),
      ArticleInput("2", "Title 2", "Content 2")
    )

    val response = parser.parse(responseText, articles)

    response.results shouldHaveSize 2
    response.results[0].id shouldBe "1"
    response.results[1].id shouldBe "2"
  }

  test("should handle empty response with createFailureResponse") {
    val articles = listOf(ArticleInput("1", "Title", "Content"))

    val response = parser.parse("", articles)

    response.results shouldHaveSize 1
    response.results[0].id shouldBe "1"
    response.results[0].success shouldBe false
    response.results[0].error shouldBe "Empty response from API"
  }

  test("should handle malformed JSON with recovery") {
    val responseText = """
      {
        "results": [
          {
            "id": "1",
            "success": true,
            "summary": "Summary 1",
            "categories": ["AI"],
            "preview": "Preview 1"
          },
          {
            "id": "2",
            "success": true,
            "summary": "Summary 2 truncated...
    """.trimIndent()
    val articles = listOf(
      ArticleInput("1", "Title 1", "Content 1"),
      ArticleInput("2", "Title 2", "Content 2")
    )

    val response = parser.parse(responseText, articles)

    response.results shouldHaveSize 2
    response.results.any { !it.success } shouldBe true
  }

  test("should create failure response for all articles when JSON is completely invalid") {
    val responseText = "This is not JSON at all"
    val articles = listOf(
      ArticleInput("1", "Title 1", "Content 1"),
      ArticleInput("2", "Title 2", "Content 2")
    )

    val response = parser.parse(responseText, articles)

    response.results shouldHaveSize 2
    response.results.all { !it.success } shouldBe true
  }

  test("should handle response with failed results") {
    val responseText = """
      {
        "results": [
          {
            "id": "1",
            "success": false,
            "error": "Processing failed"
          }
        ]
      }
    """.trimIndent()
    val articles = listOf(ArticleInput("1", "Title", "Content"))

    val response = parser.parse(responseText, articles)

    response.results shouldHaveSize 1
    response.results[0].id shouldBe "1"
    response.results[0].success shouldBe false
    response.results[0].error shouldBe "Processing failed"
  }

  test("should handle response with mixed success and failure") {
    val responseText = """
      {
        "results": [
          {
            "id": "1",
            "success": true,
            "summary": "Summary 1",
            "categories": ["AI"],
            "preview": "Preview 1"
          },
          {
            "id": "2",
            "success": false,
            "error": "Failed to process"
          }
        ]
      }
    """.trimIndent()
    val articles = listOf(
      ArticleInput("1", "Title 1", "Content 1"),
      ArticleInput("2", "Title 2", "Content 2")
    )

    val response = parser.parse(responseText, articles)

    response.results shouldHaveSize 2
    response.results[0].success shouldBe true
    response.results[1].success shouldBe false
  }

  test("should recover complete JSON object from partial response") {
    val responseText = """
      {"results":[{"id":"1","success":true,"summary":"Complete summary","categories":["AI"],"preview":"Complete preview"},{"id":"2","success":true,"summary":"Incomplete
    """.trimIndent()
    val articles = listOf(
      ArticleInput("1", "Title 1", "Content 1"),
      ArticleInput("2", "Title 2", "Content 2")
    )

    val response = parser.parse(responseText, articles)

    response.results shouldHaveSize 2
    response.results[0].id shouldBe "1"
    response.results[0].success shouldBe true
    response.results[1].id shouldBe "2"
    response.results[1].success shouldBe false
  }

  test("should handle whitespace in JSON") {
    val responseText = """
      {

        "results"  :  [

          {
            "id"  :  "1"  ,
            "success"  :  true  ,
            "summary"  :  "Summary"  ,
            "categories"  :  [  "AI"  ]  ,
            "preview"  :  "Preview"
          }

        ]

      }
    """.trimIndent()
    val articles = listOf(ArticleInput("1", "Title", "Content"))

    val response = parser.parse(responseText, articles)

    response.results shouldHaveSize 1
    response.results[0].id shouldBe "1"
    response.results[0].success shouldBe true
  }

  test("should handle empty results array") {
    val responseText = """
      {
        "results": []
      }
    """.trimIndent()
    val articles = listOf(ArticleInput("1", "Title", "Content"))

    val response = parser.parse(responseText, articles)

    response.results shouldHaveSize 0
  }

  test("should create failure response with custom error message") {
    val articles = listOf(
      ArticleInput("1", "Title 1", "Content 1"),
      ArticleInput("2", "Title 2", "Content 2")
    )
    val errorMessage = "Custom error occurred"

    val response = parser.createFailureResponse(articles, errorMessage)

    response.results shouldHaveSize 2
    response.results.all { !it.success } shouldBe true
    response.results.all { it.error == errorMessage } shouldBe true
    response.results[0].id shouldBe "1"
    response.results[1].id shouldBe "2"
  }

  test("should handle response with null values") {
    val responseText = """
      {
        "results": [
          {
            "id": "1",
            "success": true,
            "summary": null,
            "categories": null,
            "preview": null
          }
        ]
      }
    """.trimIndent()
    val articles = listOf(ArticleInput("1", "Title", "Content"))

    val response = parser.parse(responseText, articles)

    response.results shouldHaveSize 1
    response.results[0].id shouldBe "1"
    response.results[0].summary shouldBe null
    response.results[0].categories shouldBe null
    response.results[0].preview shouldBe null
  }

  test("should handle special characters in summary") {
    val responseText = """
      {
        "results": [
          {
            "id": "1",
            "success": true,
            "summary": "Summary with special chars: $, #, @, \n, \"quotes\"",
            "categories": ["AI"],
            "preview": "Preview"
          }
        ]
      }
    """.trimIndent()
    val articles = listOf(ArticleInput("1", "Title", "Content"))

    val response = parser.parse(responseText, articles)

    response.results shouldHaveSize 1
    response.results[0].summary shouldBe "Summary with special chars: $, #, @, \n, \"quotes\""
  }

  test("should handle large number of articles in failure response") {
    val articles = (1..100).map {
      ArticleInput(it.toString(), "Title $it", "Content $it")
    }

    val response = parser.createFailureResponse(articles, "Batch failed")

    response.results shouldHaveSize 100
    response.results.all { !it.success } shouldBe true
  }
})
