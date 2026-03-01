package com.techinsights.domain.service.gemini

import com.techinsights.domain.dto.gemini.ArticleInput
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.mockk.every
import io.mockk.mockk
import org.springframework.core.io.Resource

class GithubReadmePromptBuilderTest : FunSpec({

    fun buildBuilder(
        summaryTpl: String = "총 {{repo_count}}개\n{{repos}}",
        schemaTpl: String = """{"type":"object","id":"{{id}}"}""",
        itemTpl: String = "=== {{index}} ===\nID: {{id}}\n이름: {{title}}\n내용:\n{{content}}",
    ): GithubReadmePromptBuilder {
        val summaryResource = mockk<Resource> {
            every { inputStream } returns summaryTpl.byteInputStream()
        }
        val schemaResource = mockk<Resource> {
            every { inputStream } returns schemaTpl.byteInputStream()
        }
        val itemResource = mockk<Resource> {
            every { inputStream } returns itemTpl.byteInputStream()
        }
        return GithubReadmePromptBuilder(summaryResource, schemaResource, itemResource)
    }

    test("buildPrompt()는 repo_count 플레이스홀더를 아이템 수로 대체한다") {
        val builder = buildBuilder()
        val items = listOf(
            ArticleInput("owner/repo1", "repo1", "content1"),
            ArticleInput("owner/repo2", "repo2", "content2"),
        )

        val prompt = builder.buildPrompt(items)

        prompt shouldContain "총 2개"
    }

    test("buildPrompt()는 각 레포를 인덱스 순서대로 포맷한다") {
        val builder = buildBuilder()
        val items = listOf(
            ArticleInput("owner/repo1", "Repo One", "readme one"),
            ArticleInput("owner/repo2", "Repo Two", "readme two"),
        )

        val prompt = builder.buildPrompt(items)

        prompt shouldContain "=== 1 ==="
        prompt shouldContain "ID: owner/repo1"
        prompt shouldContain "이름: Repo One"
        prompt shouldContain "내용:\nreadme one"
        prompt shouldContain "=== 2 ==="
        prompt shouldContain "ID: owner/repo2"
    }

    test("buildPrompt()는 플레이스홀더를 남기지 않는다") {
        val builder = buildBuilder()
        val items = listOf(ArticleInput("owner/repo", "repo", "content"))

        val prompt = builder.buildPrompt(items)

        prompt shouldNotContain "{{repo_count}}"
        prompt shouldNotContain "{{repos}}"
        prompt shouldNotContain "{{index}}"
        prompt shouldNotContain "{{id}}"
        prompt shouldNotContain "{{title}}"
        prompt shouldNotContain "{{content}}"
    }

    test("buildSchema()는 스키마 템플릿을 그대로 반환한다") {
        val schema = """{"type":"object","results":[]}"""
        val builder = buildBuilder(schemaTpl = schema)

        builder.buildSchema() shouldBe schema
    }

    test("buildPrompt()는 빈 리스트에 대해 0개를 포함한다") {
        val builder = buildBuilder()

        val prompt = builder.buildPrompt(emptyList())

        prompt shouldContain "총 0개"
    }
})
