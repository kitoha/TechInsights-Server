package com.techinsights.batch.github.community.collect.writer

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.techinsights.batch.github.community.dto.CommunityBuzzInput
import com.techinsights.domain.dto.community.CommunityPost
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.verify
import org.springframework.batch.item.Chunk
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

class CommunityCollectWriterTest : FunSpec({

    val jdbcTemplate = mockk<NamedParameterJdbcTemplate>(relaxed = true)

    beforeTest { clearAllMocks() }

    test("빈 청크는 DB 호출 없음") {
        val writer = CommunityCollectWriter(jdbcTemplate, jacksonObjectMapper())

        writer.write(Chunk(emptyList()))

        verify(exactly = 0) { jdbcTemplate.update(any<String>(), any<MapSqlParameterSource>()) }
        verify(exactly = 0) { jdbcTemplate.batchUpdate(any<String>(), any<Array<MapSqlParameterSource>>()) }
    }

    test("totalMentions가 0이면 NO_MENTIONS SQL을 실행한다") {
        val input = CommunityBuzzInput(
            repoFullName = "owner/repo",
            ownerName = "owner",
            repoName = "repo",
            hnPosts = emptyList(),
            redditPosts = emptyList(),
            prevMentionCount = null,
            updateCount = 0,
        )

        val writer = CommunityCollectWriter(jdbcTemplate, jacksonObjectMapper())
        writer.write(Chunk(listOf(input)))

        val capturedSqls = mutableListOf<String>()
        val capturedParams = mutableListOf<MapSqlParameterSource>()
        verify(exactly = 1) {
            jdbcTemplate.batchUpdate(
                withArg { sql -> capturedSqls.add(sql) },
                withArg<Array<MapSqlParameterSource>> { params -> capturedParams.addAll(params.toList()) }
            )
        }

        capturedSqls[0].contains("NO_MENTIONS") shouldBe true
        capturedParams[0].getValue("fullName") shouldBe "owner/repo"
    }

    test("totalMentions가 0이면 community_raw_mention_count를 0으로 업데이트한다") {
        val input = CommunityBuzzInput(
            repoFullName = "owner/repo",
            ownerName = "owner",
            repoName = "repo",
            hnPosts = emptyList(),
            redditPosts = emptyList(),
            prevMentionCount = null,
            updateCount = 0,
        )

        val writer = CommunityCollectWriter(jdbcTemplate, jacksonObjectMapper())
        writer.write(Chunk(listOf(input)))

        val capturedSqls = mutableListOf<String>()
        val capturedParams = mutableListOf<MapSqlParameterSource>()
        verify(exactly = 1) {
            jdbcTemplate.batchUpdate(
                withArg { sql -> capturedSqls.add(sql) },
                withArg<Array<MapSqlParameterSource>> { params -> capturedParams.addAll(params.toList()) }
            )
        }

        capturedSqls[0].contains("community_raw_mention_count") shouldBe true
    }

    test("posts가 있으면 PENDING SQL을 실행하고 highlights JSON을 저장한다") {
        val hnPost = CommunityPost("hn", "HN Title", 100, 50, "user1", "https://hn/1")
        val redditPost = CommunityPost("reddit", "Reddit Title", 200, 80, "redditor1", "https://reddit/1")
        val input = CommunityBuzzInput(
            repoFullName = "owner/repo",
            ownerName = "owner",
            repoName = "repo",
            hnPosts = listOf(hnPost),
            redditPosts = listOf(redditPost),
            prevMentionCount = null,
            updateCount = 0,
        )

        val writer = CommunityCollectWriter(jdbcTemplate, jacksonObjectMapper())
        writer.write(Chunk(listOf(input)))

        val capturedSqls = mutableListOf<String>()
        val capturedParams = mutableListOf<MapSqlParameterSource>()
        verify(exactly = 1) {
            jdbcTemplate.batchUpdate(
                withArg { sql -> capturedSqls.add(sql) },
                withArg<Array<MapSqlParameterSource>> { params -> capturedParams.addAll(params.toList()) }
            )
        }

        capturedSqls[0].contains("PENDING") shouldBe true
        capturedParams[0].getValue("fullName") shouldBe "owner/repo"
        val highlights = capturedParams[0].getValue("highlights") as String
        highlights.contains("hnPosts") shouldBe true
        highlights.contains("redditPosts") shouldBe true
        highlights.contains("HN Title") shouldBe true
        highlights.contains("Reddit Title") shouldBe true
    }

    test("posts 있는 경우 rawMentionCount를 전체 언급 수로 설정한다") {
        val hnPost1 = CommunityPost("hn", "HN 1", 100, 50, "user1", "https://hn/1")
        val hnPost2 = CommunityPost("hn", "HN 2", 80, 30, "user2", "https://hn/2")
        val redditPost = CommunityPost("reddit", "Reddit 1", 200, 80, "redditor1", "https://reddit/1")
        val input = CommunityBuzzInput(
            repoFullName = "owner/repo",
            ownerName = "owner",
            repoName = "repo",
            hnPosts = listOf(hnPost1, hnPost2),
            redditPosts = listOf(redditPost),
            prevMentionCount = null,
            updateCount = 0,
        )

        val writer = CommunityCollectWriter(jdbcTemplate, jacksonObjectMapper())
        writer.write(Chunk(listOf(input)))

        val capturedParams = mutableListOf<MapSqlParameterSource>()
        verify(exactly = 1) {
            jdbcTemplate.batchUpdate(
                any<String>(),
                withArg<Array<MapSqlParameterSource>> { params -> capturedParams.addAll(params.toList()) }
            )
        }

        capturedParams[0].getValue("rawMentionCount") shouldBe 3
    }

    test("NO_MENTIONS와 PENDING 항목이 혼재하면 각각 별도 SQL 배치로 실행한다") {
        val noMentionsInput = CommunityBuzzInput(
            repoFullName = "owner/repo1",
            ownerName = "owner",
            repoName = "repo1",
            hnPosts = emptyList(),
            redditPosts = emptyList(),
            prevMentionCount = null,
            updateCount = 0,
        )
        val pendingInput = CommunityBuzzInput(
            repoFullName = "owner/repo2",
            ownerName = "owner",
            repoName = "repo2",
            hnPosts = listOf(CommunityPost("hn", "Title", 10, 5, "user", "https://hn/1")),
            redditPosts = emptyList(),
            prevMentionCount = null,
            updateCount = 0,
        )

        val writer = CommunityCollectWriter(jdbcTemplate, jacksonObjectMapper())
        writer.write(Chunk(listOf(noMentionsInput, pendingInput)))

        val capturedSqls = mutableListOf<String>()
        verify(exactly = 2) {
            jdbcTemplate.batchUpdate(
                withArg { sql -> capturedSqls.add(sql) },
                any<Array<MapSqlParameterSource>>()
            )
        }

        capturedSqls.any { it.contains("NO_MENTIONS") } shouldBe true
        capturedSqls.any { it.contains("PENDING") } shouldBe true
    }

    test("community_collected_at은 항상 NOW()로 업데이트된다") {
        val input = CommunityBuzzInput(
            repoFullName = "owner/repo",
            ownerName = "owner",
            repoName = "repo",
            hnPosts = emptyList(),
            redditPosts = emptyList(),
            prevMentionCount = null,
            updateCount = 0,
        )

        val writer = CommunityCollectWriter(jdbcTemplate, jacksonObjectMapper())
        writer.write(Chunk(listOf(input)))

        val capturedSqls = mutableListOf<String>()
        verify(exactly = 1) {
            jdbcTemplate.batchUpdate(
                withArg { sql -> capturedSqls.add(sql) },
                any<Array<MapSqlParameterSource>>()
            )
        }

        capturedSqls[0].contains("community_collected_at") shouldBe true
        capturedSqls[0].contains("NOW()") shouldBe true
    }
})
