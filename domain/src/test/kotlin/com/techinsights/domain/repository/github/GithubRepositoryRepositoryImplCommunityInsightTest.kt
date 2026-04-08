package com.techinsights.domain.repository.github

import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQuery
import com.querydsl.jpa.impl.JPAQueryFactory
import com.techinsights.domain.entity.github.GithubRepository
import com.techinsights.domain.entity.github.QGithubRepository
import com.techinsights.domain.enums.CommunityStatus
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.LocalDateTime

class GithubRepositoryRepositoryImplCommunityInsightTest : FunSpec({

    val jpaRepository = mockk<GithubRepositoryJpaRepository>()
    val queryFactory = mockk<JPAQueryFactory>()
    val repository = GithubRepositoryRepositoryImpl(jpaRepository, queryFactory)

    val now = LocalDateTime.of(2026, 4, 8, 12, 0)

    beforeTest { clearAllMocks() }

    fun mockQuery(results: List<GithubRepository>): JPAQuery<GithubRepository> {
        val query = mockk<JPAQuery<GithubRepository>>()
        every { queryFactory.selectFrom(QGithubRepository.githubRepository) } returns query
        every { query.where(*anyVararg<BooleanExpression>()) } returns query
        every { query.orderBy(any(), any()) } returns query
        every { query.limit(any()) } returns query
        every { query.fetch() } returns results
        return query
    }

    test("findForCommunityInsight - communityFetchedAt이 null인 레포를 반환한다") {
        val entity = buildCommunityEntity(id = 1L, communityFetchedAt = null)
        mockQuery(listOf(entity))

        val result = repository.findForCommunityInsight(pageSize = 10, afterFetchedAt = null, afterId = null)

        result shouldHaveSize 1
        result[0].id shouldBe 1L
    }

    test("findForCommunityInsight - 빈 결과를 반환할 수 있다") {
        mockQuery(emptyList())

        val result = repository.findForCommunityInsight(pageSize = 10, afterFetchedAt = null, afterId = null)

        result.shouldBeEmpty()
    }

    test("findForCommunityInsight - pageSize만큼 limit을 설정한다") {
        val query = mockQuery(emptyList())

        repository.findForCommunityInsight(pageSize = 50, afterFetchedAt = null, afterId = null)

        verify { query.limit(50L) }
    }

    test("findForCommunityInsight - afterFetchedAt과 afterId가 모두 있을 때 cursor 조건을 적용한다") {
        val entity = buildCommunityEntity(id = 5L, communityFetchedAt = now.plusDays(1))
        mockQuery(listOf(entity))

        val result = repository.findForCommunityInsight(
            pageSize = 10,
            afterFetchedAt = now,
            afterId = 3L,
        )

        result shouldHaveSize 1
    }

    test("findForCommunityInsight - afterId만 있을 때 null 구간 cursor 조건을 적용한다") {
        val entity = buildCommunityEntity(id = 10L, communityFetchedAt = now)
        mockQuery(listOf(entity))

        val result = repository.findForCommunityInsight(
            pageSize = 10,
            afterFetchedAt = null,
            afterId = 5L,
        )

        result shouldHaveSize 1
    }

    test("findForCommunityInsight - DTO의 communityFetchedAt이 엔티티와 일치한다") {
        val entity = buildCommunityEntity(id = 1L, communityFetchedAt = now)
        mockQuery(listOf(entity))

        val result = repository.findForCommunityInsight(pageSize = 10, afterFetchedAt = null, afterId = null)

        result[0].communityFetchedAt shouldBe now
    }

    test("findForCommunityInsight - communityStatus가 COMPLETED인 레포 DTO로 변환된다") {
        val entity = buildCommunityEntity(id = 1L, communityStatus = CommunityStatus.COMPLETED)
        mockQuery(listOf(entity))

        val result = repository.findForCommunityInsight(pageSize = 10, afterFetchedAt = null, afterId = null)

        result[0].communityStatus shouldBe CommunityStatus.COMPLETED
    }
})

private fun buildCommunityEntity(
    id: Long,
    communityFetchedAt: LocalDateTime? = null,
    communityStatus: CommunityStatus? = null,
    communityMentionCount: Int? = null,
    communityUpdateCount: Int = 0,
): GithubRepository = GithubRepository(
    id = id,
    repoName = "repo$id",
    fullName = "owner/repo$id",
    description = "desc",
    htmlUrl = "https://github.com/owner/repo$id",
    starCount = 1000L,
    forkCount = 10L,
    primaryLanguage = "Kotlin",
    ownerName = "owner",
    ownerAvatarUrl = null,
    topics = null,
    pushedAt = LocalDateTime.of(2024, 1, 1, 0, 0),
    fetchedAt = LocalDateTime.of(2024, 1, 2, 0, 0),
    weeklyStarDelta = 0L,
    dailyStarDelta = 0L,
).also {
    it.communityFetchedAt = communityFetchedAt
    it.communityStatus = communityStatus
    it.communityMentionCount = communityMentionCount
    it.communityUpdateCount = communityUpdateCount
}
