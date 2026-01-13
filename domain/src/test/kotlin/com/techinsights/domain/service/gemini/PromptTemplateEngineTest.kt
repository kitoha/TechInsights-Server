package com.techinsights.domain.service.gemini

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.mockk.every
import io.mockk.mockk
import org.springframework.core.io.Resource
import java.io.ByteArrayInputStream

class PromptTemplateEngineTest : FunSpec({

  val summaryTemplate = """
    Available categories: {{categories}}

    Article content:
    {{article}}
  """.trimIndent()

  val schemaTemplate = """
    {
      "categories": {{enumValues}}
    }
  """.trimIndent()

  val summaryResource = mockk<Resource>()
  val schemaResource = mockk<Resource>()

  beforeTest {
    every { summaryResource.inputStream } returns ByteArrayInputStream(summaryTemplate.toByteArray())
    every { schemaResource.inputStream } returns ByteArrayInputStream(schemaTemplate.toByteArray())
  }

  test("should build prompt with categories and article") {
    val engine = PromptTemplateEngine(summaryResource, schemaResource)
    val categories = listOf("AI", "Backend", "Frontend")
    val article = "This is a test article about technology."

    val prompt = engine.buildPrompt(article, categories)

    prompt shouldContain "AI|Backend|Frontend"
    prompt shouldContain article
    prompt shouldNotContain "{{categories}}"
    prompt shouldNotContain "{{article}}"
  }

  test("should build prompt with single category") {
    val engine = PromptTemplateEngine(summaryResource, schemaResource)
    val categories = listOf("AI")
    val article = "AI article content"

    val prompt = engine.buildPrompt(article, categories)

    prompt shouldContain "AI"
    prompt shouldContain article
  }

  test("should build prompt with empty categories") {
    val engine = PromptTemplateEngine(summaryResource, schemaResource)
    val categories = emptyList<String>()
    val article = "Article without categories"

    val prompt = engine.buildPrompt(article, categories)

    prompt shouldContain article
    prompt shouldNotContain "{{article}}"
  }

  test("should build schema with categories") {
    val engine = PromptTemplateEngine(summaryResource, schemaResource)
    val categories = listOf("AI", "Backend")

    val schema = engine.buildSchema(categories)

    schema shouldContain "[\"AI\",\"Backend\"]"
    schema shouldNotContain "{{enumValues}}"
  }

  test("should build schema with single category") {
    val engine = PromptTemplateEngine(summaryResource, schemaResource)
    val categories = listOf("AI")

    val schema = engine.buildSchema(categories)

    schema shouldContain "[\"AI\"]"
  }

  test("should build schema with empty categories") {
    val engine = PromptTemplateEngine(summaryResource, schemaResource)
    val categories = emptyList<String>()

    val schema = engine.buildSchema(categories)

    schema shouldContain "[]"
  }

  test("should handle special characters in article") {
    val engine = PromptTemplateEngine(summaryResource, schemaResource)
    val categories = listOf("AI")
    val article = "Article with special chars: \$, #, @, %, &"

    val prompt = engine.buildPrompt(article, categories)

    prompt shouldContain article
  }

  test("should handle multiline article") {
    val engine = PromptTemplateEngine(summaryResource, schemaResource)
    val categories = listOf("AI")
    val article = """
      Line 1
      Line 2
      Line 3
    """.trimIndent()

    val prompt = engine.buildPrompt(article, categories)

    prompt shouldContain "Line 1"
    prompt shouldContain "Line 2"
    prompt shouldContain "Line 3"
  }

  test("should handle Korean text in categories and article") {
    val engine = PromptTemplateEngine(summaryResource, schemaResource)
    val categories = listOf("인공지능", "백엔드")
    val article = "한글로 작성된 기술 블로그 내용입니다."

    val prompt = engine.buildPrompt(article, categories)

    prompt shouldContain "인공지능|백엔드"
    prompt shouldContain article
  }
})
