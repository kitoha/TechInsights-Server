package com.techinsights.domain.service.gemini

import com.techinsights.domain.dto.community.CommunityAnalysisInput
import com.techinsights.domain.dto.community.CommunityPost
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.mockk.every
import io.mockk.mockk
import org.springframework.core.io.Resource

class CommunityBuzzPromptBuilderTest : FunSpec({

    fun buildBuilder(
        summaryTpl: String = "총 {{repo_count}}개\n{{repos}}",
        schemaTpl: String = """{"type":"object","properties":{"results":{"sentiment":{},"insights":[]}}}""",
        itemTpl: String = "=== {{index}} ===\nID: {{id}}\n이름: {{name}}\n게시글:\n{{posts}}",
        postLineTpl: String = "[{{platform}}] {{title}} 점수:{{score}} 댓글:{{comment_count}}",
    ): CommunityBuzzPromptBuilder {
        fun mockResource(content: String) = mockk<Resource> {
            every { inputStream } returns content.byteInputStream()
        }
        return CommunityBuzzPromptBuilder(
            summaryTplResource = mockResource(summaryTpl),
            schemaTplResource = mockResource(schemaTpl),
            itemTplResource = mockResource(itemTpl),
            postLineTplResource = mockResource(postLineTpl),
        )
    }

    fun hnPost(title: String, score: Int = 10) = CommunityPost(
        platform = "hn", title = title, score = score, commentCount = 3, username = "user", url = "https://news.ycombinator.com/item?id=1"
    )

    fun redditPost(title: String, score: Int = 10) = CommunityPost(
        platform = "reddit", title = title, score = score, commentCount = 2, username = "user", url = "https://www.reddit.com/r/test/1"
    )

    fun input(
        repoFullName: String = "owner/repo",
        repoName: String = "repo",
        hnPosts: List<CommunityPost> = emptyList(),
        redditPosts: List<CommunityPost> = emptyList(),
    ) = CommunityAnalysisInput(repoFullName, repoName, hnPosts, redditPosts)

    context("buildPrompt() - 플레이스홀더 치환") {

        test("repo_count 플레이스홀더를 아이템 수로 대체한다") {
            val builder = buildBuilder()
            val items = listOf(input("owner/repo1"), input("owner/repo2"))

            builder.buildPrompt(items) shouldContain "총 2개"
        }

        test("빈 리스트에 대해 0개를 포함한다") {
            val builder = buildBuilder()

            builder.buildPrompt(emptyList()) shouldContain "총 0개"
        }

        test("플레이스홀더가 프롬프트에 남지 않는다") {
            val builder = buildBuilder()
            val items = listOf(input(hnPosts = listOf(hnPost("Post"))))

            val prompt = builder.buildPrompt(items)

            prompt shouldNotContain "{{repo_count}}"
            prompt shouldNotContain "{{repos}}"
            prompt shouldNotContain "{{index}}"
            prompt shouldNotContain "{{id}}"
            prompt shouldNotContain "{{name}}"
            prompt shouldNotContain "{{posts}}"
            prompt shouldNotContain "{{platform}}"
            prompt shouldNotContain "{{title}}"
            prompt shouldNotContain "{{score}}"
            prompt shouldNotContain "{{comment_count}}"
        }
    }

    context("buildPrompt() - 레포 정보") {

        test("repoFullName이 프롬프트에 포함된다") {
            val builder = buildBuilder()

            val prompt = builder.buildPrompt(listOf(input("facebook/react")))

            prompt shouldContain "ID: facebook/react"
        }

        test("repoName이 프롬프트에 포함된다") {
            val builder = buildBuilder()

            val prompt = builder.buildPrompt(listOf(input(repoName = "react")))

            prompt shouldContain "이름: react"
        }

        test("여러 레포가 모두 인덱스 순서로 포함된다") {
            val builder = buildBuilder()
            val items = listOf(input("owner/repo1"), input("owner/repo2"), input("owner/repo3"))

            val prompt = builder.buildPrompt(items)

            prompt shouldContain "=== 1 ==="
            prompt shouldContain "ID: owner/repo1"
            prompt shouldContain "=== 2 ==="
            prompt shouldContain "ID: owner/repo2"
            prompt shouldContain "=== 3 ==="
            prompt shouldContain "ID: owner/repo3"
        }
    }

    context("buildPrompt() - 게시글 포맷") {

        test("HN 게시글 제목이 프롬프트에 포함된다") {
            val builder = buildBuilder()
            val items = listOf(input(hnPosts = listOf(hnPost("Great open source tool"))))

            builder.buildPrompt(items) shouldContain "Great open source tool"
        }

        test("Reddit 게시글 제목이 프롬프트에 포함된다") {
            val builder = buildBuilder()
            val items = listOf(input(redditPosts = listOf(redditPost("Reddit discussion"))))

            builder.buildPrompt(items) shouldContain "Reddit discussion"
        }

        test("HN과 Reddit 게시글이 모두 포함된다") {
            val builder = buildBuilder()
            val items = listOf(input(
                hnPosts = listOf(hnPost("HN Post")),
                redditPosts = listOf(redditPost("Reddit Post")),
            ))

            val prompt = builder.buildPrompt(items)

            prompt shouldContain "HN Post"
            prompt shouldContain "Reddit Post"
        }

        test("점수와 댓글 수가 postLineTpl에 따라 포함된다") {
            val builder = buildBuilder(postLineTpl = "[{{platform}}] {{title}} 점수:{{score}} 댓글:{{comment_count}}")
            val items = listOf(input(hnPosts = listOf(
                CommunityPost("hn", "Post", score = 42, commentCount = 7, username = "user", url = "https://example.com")
            )))

            val prompt = builder.buildPrompt(items)

            prompt shouldContain "점수:42"
            prompt shouldContain "댓글:7"
        }

        test("postLineTpl 포맷이 게시글마다 적용된다") {
            val builder = buildBuilder(postLineTpl = "PLATFORM={{platform}} TITLE={{title}}")
            val items = listOf(input(
                hnPosts = listOf(hnPost("HN Post")),
                redditPosts = listOf(redditPost("Reddit Post")),
            ))

            val prompt = builder.buildPrompt(items)

            prompt shouldContain "PLATFORM=hn TITLE=HN Post"
            prompt shouldContain "PLATFORM=reddit TITLE=Reddit Post"
        }
    }

    context("buildPrompt() - 플레이스홀더 문자열이 입력값에 포함된 경우") {

        test("제목에 {{score}} 형태의 문자열이 있어도 실제 점수 치환에 영향을 주지 않는다") {
            val builder = buildBuilder(postLineTpl = "[{{platform}}] {{title}} 점수:{{score}} 댓글:{{comment_count}}")
            val items = listOf(input(hnPosts = listOf(
                CommunityPost("hn", "{{score}} is a cool metric", score = 42, commentCount = 7, username = "user", url = "https://example.com")
            )))

            val prompt = builder.buildPrompt(items)

            prompt shouldContain "{{score}} is a cool metric"
            prompt shouldContain "점수:42"
        }

        test("제목에 {{comment_count}} 형태의 문자열이 있어도 실제 댓글 수 치환에 영향을 주지 않는다") {
            val builder = buildBuilder(postLineTpl = "[{{platform}}] {{title}} 점수:{{score}} 댓글:{{comment_count}}")
            val items = listOf(input(hnPosts = listOf(
                CommunityPost("hn", "{{comment_count}} comments expected", score = 10, commentCount = 5, username = "user", url = "https://example.com")
            )))

            val prompt = builder.buildPrompt(items)

            prompt shouldContain "{{comment_count}} comments expected"
            prompt shouldContain "댓글:5"
        }

        test("플랫폼 값에 {{title}} 형태의 문자열이 있어도 제목 치환에 영향을 주지 않는다") {
            val builder = buildBuilder(postLineTpl = "[{{platform}}] {{title}}")
            val items = listOf(input(hnPosts = listOf(
                CommunityPost("{{title}}", "actual title", score = 1, commentCount = 0, username = "user", url = "https://example.com")
            )))

            val prompt = builder.buildPrompt(items)

            prompt shouldContain "[{{title}}]"
            prompt shouldContain "actual title"
        }
    }

    context("buildSchema()") {

        test("스키마 템플릿을 그대로 반환한다") {
            val schema = """{"type":"object","properties":{"results":{"sentiment":{},"insights":[]}}}"""
            val builder = buildBuilder(schemaTpl = schema)

            builder.buildSchema() shouldBe schema
        }
    }
})
