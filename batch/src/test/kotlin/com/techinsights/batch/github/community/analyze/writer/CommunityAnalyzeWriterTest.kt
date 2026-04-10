package com.techinsights.batch.github.community.analyze.writer

import com.techinsights.domain.dto.community.CommunityAnalysisInput
import com.techinsights.domain.dto.community.CommunityAnalysisResult
import com.techinsights.domain.dto.community.CommunityPost
import com.techinsights.domain.service.gemini.CommunityAnalyzer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import org.springframework.batch.item.Chunk
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

class CommunityAnalyzeWriterTest : FunSpec({

    val analyzer = mockk<CommunityAnalyzer>()
    val jdbcTemplate = mockk<NamedParameterJdbcTemplate>(relaxed = true)

    beforeTest { clearAllMocks() }

    test("빈 청크는 analyzer를 호출하지 않는다") {
        val writer = CommunityAnalyzeWriter(analyzer, jdbcTemplate)

        writer.write(Chunk(emptyList()))

        verify(exactly = 0) { analyzer.analyze(any()) }
        verify(exactly = 0) { jdbcTemplate.batchUpdate(any<String>(), any<Array<MapSqlParameterSource>>()) }
    }

    test("성공 결과는 COMPLETED SQL로 sentiment, insights, highlights, mentionCount를 저장한다") {
        val hnPost = CommunityPost("hn", "HN Title", 100, 50, "user1", "https://hn/1")
        val redditPost = CommunityPost("reddit", "Reddit Title", 200, 80, "redditor1", "https://reddit/1")
        val input = CommunityAnalysisInput(
            repoFullName = "owner/repo",
            repoName = "repo",
            hnPosts = listOf(hnPost),
            redditPosts = listOf(redditPost),
        )
        val result = CommunityAnalysisResult(
            id = "owner/repo",
            success = true,
            sentiment = CommunityAnalysisResult.SentimentBreakdown(positive = 69, neutral = 26, negative = 5),
            insights = listOf(CommunityAnalysisResult.CommunityInsight(text = "Good project", tone = "positive")),
        )

        every { analyzer.analyze(listOf(input)) } returns flowOf(result)

        val writer = CommunityAnalyzeWriter(analyzer, jdbcTemplate)
        writer.write(Chunk(listOf(input)))

        val capturedSqls = mutableListOf<String>()
        val capturedParams = mutableListOf<MapSqlParameterSource>()
        verify(exactly = 1) {
            jdbcTemplate.batchUpdate(
                withArg { sql -> capturedSqls.add(sql) },
                withArg<Array<MapSqlParameterSource>> { params -> capturedParams.addAll(params.toList()) }
            )
        }

        capturedSqls[0].contains("COMPLETED") shouldBe true
        capturedParams[0].getValue("id") shouldBe "owner/repo"
        capturedParams[0].getValue("mentionCount") shouldBe 2  // 1 hn + 1 reddit
        (capturedParams[0].getValue("sentiment") as String).contains("69") shouldBe true
        (capturedParams[0].getValue("insights") as String).contains("Good project") shouldBe true
        (capturedParams[0].getValue("highlights") as String).contains("HN Title") shouldBe true
        (capturedParams[0].getValue("highlights") as String).contains("Reddit Title") shouldBe true
    }

    test("실패 결과는 FAILED SQL로 저장한다") {
        val input = CommunityAnalysisInput(
            repoFullName = "owner/repo",
            repoName = "repo",
            hnPosts = emptyList(),
            redditPosts = emptyList(),
        )
        val result = CommunityAnalysisResult(
            id = "owner/repo",
            success = false,
            error = "Gemini API error",
        )

        every { analyzer.analyze(listOf(input)) } returns flowOf(result)

        val writer = CommunityAnalyzeWriter(analyzer, jdbcTemplate)
        writer.write(Chunk(listOf(input)))

        val capturedSqls = mutableListOf<String>()
        val capturedParams = mutableListOf<MapSqlParameterSource>()
        verify(exactly = 1) {
            jdbcTemplate.batchUpdate(
                withArg { sql -> capturedSqls.add(sql) },
                withArg<Array<MapSqlParameterSource>> { params -> capturedParams.addAll(params.toList()) }
            )
        }

        capturedSqls[0].contains("FAILED") shouldBe true
        capturedParams[0].getValue("id") shouldBe "owner/repo"
    }

    test("highlights는 hnPosts[0]과 redditPosts[0]만 저장한다 (각 플랫폼 최대 1개)") {
        val hnPost1 = CommunityPost("hn", "HN First", 100, 50, "user1", "https://hn/1")
        val hnPost2 = CommunityPost("hn", "HN Second", 80, 30, "user2", "https://hn/2")
        val redditPost1 = CommunityPost("reddit", "Reddit First", 200, 80, "redditor1", "https://reddit/1")
        val redditPost2 = CommunityPost("reddit", "Reddit Second", 150, 60, "redditor2", "https://reddit/2")
        val input = CommunityAnalysisInput(
            repoFullName = "owner/repo",
            repoName = "repo",
            hnPosts = listOf(hnPost1, hnPost2),
            redditPosts = listOf(redditPost1, redditPost2),
        )
        val result = CommunityAnalysisResult(
            id = "owner/repo",
            success = true,
            sentiment = CommunityAnalysisResult.SentimentBreakdown(50, 30, 20),
            insights = emptyList(),
        )

        every { analyzer.analyze(listOf(input)) } returns flowOf(result)

        val writer = CommunityAnalyzeWriter(analyzer, jdbcTemplate)
        writer.write(Chunk(listOf(input)))

        val capturedParams = mutableListOf<MapSqlParameterSource>()
        verify(exactly = 1) {
            jdbcTemplate.batchUpdate(
                any<String>(),
                withArg<Array<MapSqlParameterSource>> { params -> capturedParams.addAll(params.toList()) }
            )
        }

        val highlights = capturedParams[0].getValue("highlights") as String
        highlights.contains("HN First") shouldBe true
        highlights.contains("HN Second") shouldBe false
        highlights.contains("Reddit First") shouldBe true
        highlights.contains("Reddit Second") shouldBe false
    }

    test("hnPosts가 없으면 highlights에 redditPosts[0]만 포함된다") {
        val redditPost = CommunityPost("reddit", "Reddit Only", 200, 80, "redditor1", "https://reddit/1")
        val input = CommunityAnalysisInput(
            repoFullName = "owner/repo",
            repoName = "repo",
            hnPosts = emptyList(),
            redditPosts = listOf(redditPost),
        )
        val result = CommunityAnalysisResult(
            id = "owner/repo",
            success = true,
            sentiment = CommunityAnalysisResult.SentimentBreakdown(50, 30, 20),
            insights = emptyList(),
        )

        every { analyzer.analyze(listOf(input)) } returns flowOf(result)

        val writer = CommunityAnalyzeWriter(analyzer, jdbcTemplate)
        writer.write(Chunk(listOf(input)))

        val capturedParams = mutableListOf<MapSqlParameterSource>()
        verify(exactly = 1) {
            jdbcTemplate.batchUpdate(
                any<String>(),
                withArg<Array<MapSqlParameterSource>> { params -> capturedParams.addAll(params.toList()) }
            )
        }

        val highlights = capturedParams[0].getValue("highlights") as String
        highlights.contains("Reddit Only") shouldBe true
        highlights.contains("hn") shouldBe false
    }

    test("analyzer가 예외를 던지면 모든 항목을 FAILED로 저장한다") {
        val input1 = CommunityAnalysisInput("owner/repo1", "repo1", emptyList(), emptyList())
        val input2 = CommunityAnalysisInput("owner/repo2", "repo2", emptyList(), emptyList())

        every { analyzer.analyze(any()) } throws RuntimeException("Gemini unavailable")

        val writer = CommunityAnalyzeWriter(analyzer, jdbcTemplate)
        writer.write(Chunk(listOf(input1, input2)))

        val capturedSqls = mutableListOf<String>()
        val capturedParams = mutableListOf<MapSqlParameterSource>()
        verify(exactly = 1) {
            jdbcTemplate.batchUpdate(
                withArg { sql -> capturedSqls.add(sql) },
                withArg<Array<MapSqlParameterSource>> { params -> capturedParams.addAll(params.toList()) }
            )
        }

        capturedSqls[0].contains("FAILED") shouldBe true
        capturedParams.size shouldBe 2
        capturedParams.map { it.getValue("id") }.toSet() shouldBe setOf("owner/repo1", "owner/repo2")
    }
})
