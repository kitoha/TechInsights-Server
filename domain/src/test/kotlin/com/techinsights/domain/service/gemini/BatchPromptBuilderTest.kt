package com.techinsights.domain.service.gemini

import com.techinsights.domain.dto.gemini.ArticleInput
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.mockk.every
import io.mockk.mockk
import org.springframework.core.io.Resource
import java.io.ByteArrayInputStream

class BatchPromptBuilderTest : FunSpec({

  val summaryTemplate = """
    Processing {{article_count}} articles.
    Categories: {{categories}}

    {{articles}}
  """.trimIndent()

  val schemaTemplate = """
    {"categories": {{enumValues}}}
  """.trimIndent()

  val articleItemTemplate = """
    Article #{{index}}
    ID: {{id}}
    Title: {{title}}
    Content: {{content}}
  """.trimIndent()

  val summaryResource = mockk<Resource>()
  val schemaResource = mockk<Resource>()
  val articleItemResource = mockk<Resource>()

  beforeTest {
    every { summaryResource.inputStream } returns ByteArrayInputStream(summaryTemplate.toByteArray())
    every { schemaResource.inputStream } returns ByteArrayInputStream(schemaTemplate.toByteArray())
    every { articleItemResource.inputStream } returns ByteArrayInputStream(articleItemTemplate.toByteArray())
  }

  test("should build prompt with multiple articles") {
    val builder = BatchPromptBuilder(summaryResource, schemaResource, articleItemResource)
    val articles = listOf(
      ArticleInput("1", "Title 1", "Content 1"),
      ArticleInput("2", "Title 2", "Content 2")
    )
    val categories = listOf("AI", "Backend")

    val prompt = builder.buildPrompt(articles, categories)

    prompt shouldContain "Processing 2 articles"
    prompt shouldContain "AI, Backend"
    prompt shouldContain "Article #1"
    prompt shouldContain "ID: 1"
    prompt shouldContain "Title: Title 1"
    prompt shouldContain "Content: Content 1"
    prompt shouldContain "Article #2"
    prompt shouldContain "ID: 2"
  }

  test("should build prompt with single article") {
    val builder = BatchPromptBuilder(summaryResource, schemaResource, articleItemResource)
    val articles = listOf(
      ArticleInput("1", "Single Title", "Single Content")
    )
    val categories = listOf("AI")

    val prompt = builder.buildPrompt(articles, categories)

    prompt shouldContain "Processing 1 articles"
    prompt shouldContain "AI"
    prompt shouldContain "Article #1"
    prompt shouldContain "ID: 1"
  }

  test("should build schema with categories") {
    val builder = BatchPromptBuilder(summaryResource, schemaResource, articleItemResource)
    val categories = listOf("AI", "Backend", "Frontend")

    val schema = builder.buildSchema(categories)

    schema shouldContain "[\"AI\",\"Backend\",\"Frontend\"]"
    schema shouldNotContain "{{enumValues}}"
  }

  test("should handle articles with special characters") {
    val builder = BatchPromptBuilder(summaryResource, schemaResource, articleItemResource)
    val articles = listOf(
      ArticleInput("1", "Title with special chars!", "Content with @#\$%")
    )
    val categories = listOf("AI")

    val prompt = builder.buildPrompt(articles, categories)

    prompt shouldContain "Title with special chars!"
    prompt shouldContain "Content with @#\$%"
  }

  test("should handle articles with multiline content") {
    val builder = BatchPromptBuilder(summaryResource, schemaResource, articleItemResource)
    val articles = listOf(
      ArticleInput(
        "1",
        "Multiline Title",
        """
          Line 1
          Line 2
          Line 3
        """.trimIndent()
      )
    )
    val categories = listOf("AI")

    val prompt = builder.buildPrompt(articles, categories)

    prompt shouldContain "Line 1"
    prompt shouldContain "Line 2"
    prompt shouldContain "Line 3"
  }

  test("should handle Korean text in articles") {
    val builder = BatchPromptBuilder(summaryResource, schemaResource, articleItemResource)
    val articles = listOf(
      ArticleInput("1", "한글 제목", "한글로 작성된 내용입니다.")
    )
    val categories = listOf("인공지능")

    val prompt = builder.buildPrompt(articles, categories)

    prompt shouldContain "한글 제목"
    prompt shouldContain "한글로 작성된 내용입니다."
    prompt shouldContain "인공지능"
  }

  test("should handle empty categories list") {
    val builder = BatchPromptBuilder(summaryResource, schemaResource, articleItemResource)
    val articles = listOf(
      ArticleInput("1", "Title", "Content")
    )
    val categories = emptyList<String>()

    val prompt = builder.buildPrompt(articles, categories)

    prompt shouldContain "Article #1"
  }

  test("should replace all placeholders correctly") {
    val builder = BatchPromptBuilder(summaryResource, schemaResource, articleItemResource)
    val articles = listOf(
      ArticleInput("123", "Test Title", "Test Content")
    )
    val categories = listOf("AI")

    val prompt = builder.buildPrompt(articles, categories)

    prompt shouldNotContain "{{article_count}}"
    prompt shouldNotContain "{{categories}}"
    prompt shouldNotContain "{{articles}}"
    prompt shouldNotContain "{{index}}"
    prompt shouldNotContain "{{id}}"
    prompt shouldNotContain "{{title}}"
    prompt shouldNotContain "{{content}}"
  }

  test("should build prompt with 10 articles") {
    val builder = BatchPromptBuilder(summaryResource, schemaResource, articleItemResource)
    val articles = (1..10).map {
      ArticleInput(it.toString(), "Title $it", "Content $it")
    }
    val categories = listOf("AI")

    val prompt = builder.buildPrompt(articles, categories)

    prompt shouldContain "Processing 10 articles"
    prompt shouldContain "Article #1"
    prompt shouldContain "Article #10"
  }

  test("should separate articles with double newline") {
    val builder = BatchPromptBuilder(summaryResource, schemaResource, articleItemResource)
    val articles = listOf(
      ArticleInput("1", "Title 1", "Content 1"),
      ArticleInput("2", "Title 2", "Content 2")
    )
    val categories = listOf("AI")

    val prompt = builder.buildPrompt(articles, categories)

    prompt shouldContain "Article #1"
    prompt shouldContain "Article #2"
  }
})
