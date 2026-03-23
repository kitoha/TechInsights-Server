package com.techinsights.batch.github.readme.writer

import com.techinsights.domain.dto.gemini.ArticleInput
import com.techinsights.domain.dto.gemini.SummaryResultWithId
import com.techinsights.domain.enums.ErrorType
import com.techinsights.domain.service.gemini.GithubReadmeBatchSummarizer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import org.springframework.batch.item.Chunk
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

class GithubReadmeBatchSummaryWriterTest : FunSpec({

    val summarizer = mockk<GithubReadmeBatchSummarizer>()
    val jdbcTemplate = mockk<NamedParameterJdbcTemplate>(relaxed = true)

    beforeTest { clearAllMocks() }

    test("빈 청크는 summarizer를 호출하지 않는다") {
        val writer = GithubReadmeBatchSummaryWriter(summarizer, jdbcTemplate)

        writer.write(Chunk(emptyList()))

        verify(exactly = 0) { summarizer.summarize(any()) }
    }

    test("성공한 요약만 DB에 업데이트한다") {
        val item1 = ArticleInput("owner/repo1", "repo1", "content1")
        val item2 = ArticleInput("owner/repo2", "repo2", "content2")

        every { summarizer.summarize(any()) } returns flowOf(
            SummaryResultWithId(id = "owner/repo1", success = true, summary = "요약 1"),
            SummaryResultWithId(id = "owner/repo2", success = false, error = "요약 실패"),
        )

        val writer = GithubReadmeBatchSummaryWriter(summarizer, jdbcTemplate)
        writer.write(Chunk(listOf(item1, item2)))

        val capturedParams = mutableListOf<MapSqlParameterSource>()
        verify(exactly = 2) {
            jdbcTemplate.batchUpdate(
                any<String>(),
                withArg<Array<MapSqlParameterSource>> { params ->
                    capturedParams.addAll(params.toList())
                }
            )
        }

        val successParams = capturedParams.filter { it.hasValue("summary") }
        successParams.size shouldBe 1
        successParams[0].getValue("fullName") shouldBe "owner/repo1"
        successParams[0].getValue("summary") shouldBe "요약 1"
    }

    test("모든 요약이 실패하면 실패 마킹만 한다") {
        val item = ArticleInput("owner/repo", "repo", "content")

        every { summarizer.summarize(any()) } returns flowOf(
            SummaryResultWithId(id = "owner/repo", success = false, error = "API error")
        )

        val writer = GithubReadmeBatchSummaryWriter(summarizer, jdbcTemplate)
        writer.write(Chunk(listOf(item)))

        // success UPDATE는 호출되지 않고, MARK_FAILED만 1번 호출
        val capturedParams = mutableListOf<MapSqlParameterSource>()
        verify(exactly = 1) {
            jdbcTemplate.batchUpdate(
                any<String>(),
                withArg<Array<MapSqlParameterSource>> { params ->
                    capturedParams.addAll(params.toList())
                }
            )
        }
        capturedParams.size shouldBe 1
        capturedParams[0].getValue("fullName") shouldBe "owner/repo"
        capturedParams[0].hasValue("summary") shouldBe false
    }

    test("summarizer에 청크의 모든 아이템을 전달한다") {
        val items = listOf(
            ArticleInput("owner/repo1", "repo1", "content1"),
            ArticleInput("owner/repo2", "repo2", "content2"),
            ArticleInput("owner/repo3", "repo3", "content3"),
        )

        every { summarizer.summarize(items) } returns flowOf(
            SummaryResultWithId(id = "owner/repo1", success = true, summary = "요약1"),
            SummaryResultWithId(id = "owner/repo2", success = true, summary = "요약2"),
            SummaryResultWithId(id = "owner/repo3", success = true, summary = "요약3"),
        )

        val writer = GithubReadmeBatchSummaryWriter(summarizer, jdbcTemplate)
        writer.write(Chunk(items))

        verify { summarizer.summarize(items) }
    }

    test("실패 결과의 errorType이 DB 파라미터에 저장된다") {
        val item = ArticleInput("owner/repo", "repo", "content")

        every { summarizer.summarize(any()) } returns flowOf(
            SummaryResultWithId(id = "owner/repo", success = false, error = "API error", errorType = ErrorType.API_ERROR)
        )

        val writer = GithubReadmeBatchSummaryWriter(summarizer, jdbcTemplate)
        writer.write(Chunk(listOf(item)))

        val capturedParams = mutableListOf<MapSqlParameterSource>()
        verify(exactly = 1) {
            jdbcTemplate.batchUpdate(
                any<String>(),
                withArg<Array<MapSqlParameterSource>> { params -> capturedParams.addAll(params.toList()) }
            )
        }
        capturedParams[0].getValue("errorType") shouldBe "API_ERROR"
    }

    test("errorType이 null인 실패는 UNKNOWN으로 저장된다") {
        val item = ArticleInput("owner/repo", "repo", "content")

        every { summarizer.summarize(any()) } returns flowOf(
            SummaryResultWithId(id = "owner/repo", success = false, error = "unknown error", errorType = null)
        )

        val writer = GithubReadmeBatchSummaryWriter(summarizer, jdbcTemplate)
        writer.write(Chunk(listOf(item)))

        val capturedParams = mutableListOf<MapSqlParameterSource>()
        verify(exactly = 1) {
            jdbcTemplate.batchUpdate(
                any<String>(),
                withArg<Array<MapSqlParameterSource>> { params -> capturedParams.addAll(params.toList()) }
            )
        }
        capturedParams[0].getValue("errorType") shouldBe "UNKNOWN"
    }

    test("성공 시 readme_summary_error_type을 NULL로 초기화하는 SQL을 사용한다") {
        val item = ArticleInput("owner/repo", "repo", "content")

        every { summarizer.summarize(any()) } returns flowOf(
            SummaryResultWithId(id = "owner/repo", success = true, summary = "요약")
        )

        val writer = GithubReadmeBatchSummaryWriter(summarizer, jdbcTemplate)
        writer.write(Chunk(listOf(item)))

        val capturedSqls = mutableListOf<String>()
        verify(exactly = 1) {
            jdbcTemplate.batchUpdate(
                withArg<String> { sql -> capturedSqls.add(sql) },
                any<Array<MapSqlParameterSource>>()
            )
        }
        capturedSqls[0].contains("readme_summary_error_type") shouldBe true
    }
})
