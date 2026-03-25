package com.techinsights.batch.github.writer

import com.techinsights.batch.github.dto.GithubRepoUpsertData
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.batch.item.Chunk
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.LocalDateTime

class GithubRepoWriterTest : FunSpec({

    val jdbcTemplate = mockk<NamedParameterJdbcTemplate>()
    val writer = GithubRepoWriter(jdbcTemplate)

    beforeTest { clearAllMocks() }

    test("UPSERT SQL로 batchUpdate를 호출한다") {
        val data = createUpsertData("owner/repo1")
        val chunk = Chunk(listOf(data))

        every { jdbcTemplate.batchUpdate(any<String>(), any<Array<MapSqlParameterSource>>()) } returns intArrayOf(1)

        writer.write(chunk)

        verify(exactly = 1) {
            jdbcTemplate.batchUpdate(any<String>(), any<Array<MapSqlParameterSource>>())
        }
    }

    test("여러 items를 한 번의 batchUpdate로 처리한다") {
        val items = (1..5).map { createUpsertData("owner/repo$it") }
        val chunk = Chunk(items)

        every { jdbcTemplate.batchUpdate(any<String>(), any<Array<MapSqlParameterSource>>()) } returns IntArray(5) { 1 }

        writer.write(chunk)

        verify(exactly = 1) {
            jdbcTemplate.batchUpdate(any<String>(), any<Array<MapSqlParameterSource>>())
        }
    }

    test("UPSERT SQL에 daily_star_delta 계산 로직이 포함된다") {
        val sqlSlot = slot<String>()
        every { jdbcTemplate.batchUpdate(capture(sqlSlot), any<Array<MapSqlParameterSource>>()) } returns intArrayOf(1)

        writer.write(Chunk(listOf(createUpsertData())))

        sqlSlot.captured shouldContain "daily_star_delta"
        sqlSlot.captured shouldContain "star_count_prev_day"
    }

    test("daily_star_delta 갱신 조건이 캘린더 일 기준(DATE_TRUNC)으로 비교된다") {
        val sqlSlot = slot<String>()
        every { jdbcTemplate.batchUpdate(capture(sqlSlot), any<Array<MapSqlParameterSource>>()) } returns intArrayOf(1)

        writer.write(Chunk(listOf(createUpsertData())))

        sqlSlot.captured shouldContain "DATE_TRUNC('day', NOW())"
        sqlSlot.captured shouldContain "star_count_prev_day_updated_at < DATE_TRUNC('day', NOW())"
    }

    test("weekly_star_delta 갱신 조건은 기존 INTERVAL 방식을 유지한다") {
        val sqlSlot = slot<String>()
        every { jdbcTemplate.batchUpdate(capture(sqlSlot), any<Array<MapSqlParameterSource>>()) } returns intArrayOf(1)

        writer.write(Chunk(listOf(createUpsertData())))

        sqlSlot.captured shouldContain "INTERVAL '7 days'"
    }

    test("UPSERT SQL ON CONFLICT DO UPDATE에 소프트 삭제 레포 갱신 방지 조건이 포함된다") {
        val sqlSlot = slot<String>()
        every { jdbcTemplate.batchUpdate(capture(sqlSlot), any<Array<MapSqlParameterSource>>()) } returns intArrayOf(1)

        writer.write(Chunk(listOf(createUpsertData())))

        sqlSlot.captured shouldContain "WHERE github_repositories.deleted_at IS NULL"
    }

    test("빈 chunk는 jdbcTemplate을 호출하지 않는다") {
        val chunk = Chunk(emptyList<GithubRepoUpsertData>())

        writer.write(chunk)

        verify(exactly = 0) {
            jdbcTemplate.batchUpdate(any<String>(), any<Array<MapSqlParameterSource>>())
        }
    }
})

private fun createUpsertData(fullName: String = "owner/repo"): GithubRepoUpsertData =
    GithubRepoUpsertData(
        id = fullName.hashCode().toLong(),
        repoName = fullName.substringAfter("/"),
        fullName = fullName,
        description = "A great project",
        htmlUrl = "https://github.com/$fullName",
        starCount = 1000L,
        forkCount = 200L,
        primaryLanguage = "Kotlin",
        ownerName = fullName.substringBefore("/"),
        ownerAvatarUrl = "https://avatars.githubusercontent.com/u/1",
        topics = "kotlin,spring",
        pushedAt = LocalDateTime.now(),
        fetchedAt = LocalDateTime.now(),
    )
