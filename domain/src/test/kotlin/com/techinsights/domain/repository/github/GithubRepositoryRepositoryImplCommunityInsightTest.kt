package com.techinsights.domain.repository.github

import com.querydsl.core.Tuple
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQuery
import com.querydsl.jpa.impl.JPAQueryFactory
import com.techinsights.domain.entity.github.GithubRepository
import com.techinsights.domain.entity.github.GithubRepositoryCommunity
import com.techinsights.domain.entity.github.QGithubRepository
import com.techinsights.domain.entity.github.QGithubRepositoryCommunity
import com.techinsights.domain.enums.CommunityStatus
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime

class GithubRepositoryRepositoryImplCommunityInsightTest : FunSpec({

    val jpaRepository = mockk<GithubRepositoryJpaRepository>()
    val queryFactory = mockk<JPAQueryFactory>()
    val repository = GithubRepositoryRepositoryImpl(jpaRepository, queryFactory)

    val now = LocalDateTime.of(2026, 4, 8, 12, 0)
    val normalRefreshAfter = now.minusDays(14)
    val noMentionsRefreshAfter = now.minusDays(30)

    beforeTest { clearAllMocks() }

    fun mockTupleQuery(results: List<Tuple>): JPAQuery<Tuple> {
        val query = mockk<JPAQuery<Tuple>>()
        every { queryFactory.select(any(), any()) } returns query
        every { query.from(QGithubRepository.githubRepository) } returns query
        every { query.leftJoin(QGithubRepositoryCommunity.githubRepositoryCommunity) } returns query
        every { query.join(QGithubRepositoryCommunity.githubRepositoryCommunity) } returns query
        every { query.on(any<BooleanExpression>()) } returns query
        every { query.where(*anyVararg<BooleanExpression>()) } returns query
        every { query.orderBy(any(), any()) } returns query
        every { query.orderBy(any()) } returns query
        every { query.limit(any()) } returns query
        every { query.fetch() } returns results
        return query
    }

    fun buildTuple(
        entity: GithubRepository,
        community: GithubRepositoryCommunity?,
    ): Tuple {
        val tuple = mockk<Tuple>()
        every { tuple[QGithubRepository.githubRepository] } returns entity
        every { tuple[QGithubRepositoryCommunity.githubRepositoryCommunity] } returns community
        return tuple
    }

    fun buildEntity(id: Long): GithubRepository = GithubRepository(
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
    )

    context("findForCommunityCollect") {

        test("community 행이 없는 레포를 반환한다") {
            val entity = buildEntity(id = 1L)
            val tuple = buildTuple(entity, community = null)
            mockTupleQuery(listOf(tuple))

            val result = repository.findForCommunityCollect(
                pageSize = 10,
                afterCollectedAt = null,
                afterId = null,
                noMentionsRefreshAfter = noMentionsRefreshAfter,
                normalRefreshAfter = normalRefreshAfter,
            )

            result shouldHaveSize 1
            result[0].id shouldBe 1L
        }

        test("빈 결과를 반환할 수 있다") {
            mockTupleQuery(emptyList())

            val result = repository.findForCommunityCollect(
                pageSize = 10,
                afterCollectedAt = null,
                afterId = null,
                noMentionsRefreshAfter = noMentionsRefreshAfter,
                normalRefreshAfter = normalRefreshAfter,
            )

            result.shouldBeEmpty()
        }

        test("pageSize만큼 limit을 설정한다") {
            val query = mockTupleQuery(emptyList())

            repository.findForCommunityCollect(
                pageSize = 50,
                afterCollectedAt = null,
                afterId = null,
                noMentionsRefreshAfter = noMentionsRefreshAfter,
                normalRefreshAfter = normalRefreshAfter,
            )

            verify { query.limit(50L) }
        }

        test("DTO의 communityCollectedAt이 community 엔티티 값과 일치한다") {
            val entity = buildEntity(id = 1L)
            val community = GithubRepositoryCommunity(repoId = 1L, communityCollectedAt = now)
            val tuple = buildTuple(entity, community)
            mockTupleQuery(listOf(tuple))

            val result = repository.findForCommunityCollect(
                pageSize = 10,
                afterCollectedAt = null,
                afterId = null,
                noMentionsRefreshAfter = noMentionsRefreshAfter,
                normalRefreshAfter = normalRefreshAfter,
            )

            result[0].communityCollectedAt shouldBe now
        }

        test("communityStatus가 COMPLETED인 레포 DTO로 변환된다") {
            val entity = buildEntity(id = 1L)
            val community = GithubRepositoryCommunity(repoId = 1L, communityStatus = CommunityStatus.COMPLETED)
            val tuple = buildTuple(entity, community)
            mockTupleQuery(listOf(tuple))

            val result = repository.findForCommunityCollect(
                pageSize = 10,
                afterCollectedAt = null,
                afterId = null,
                noMentionsRefreshAfter = noMentionsRefreshAfter,
                normalRefreshAfter = normalRefreshAfter,
            )

            result[0].communityStatus shouldBe CommunityStatus.COMPLETED
        }
    }

    context("findForCommunityAnalyze") {

        test("빈 결과를 반환할 수 있다") {
            mockTupleQuery(emptyList())

            val result = repository.findForCommunityAnalyze(pageSize = 10, afterId = null)

            result.shouldBeEmpty()
        }

        test("pageSize만큼 limit을 설정한다") {
            val query = mockTupleQuery(emptyList())

            repository.findForCommunityAnalyze(pageSize = 20, afterId = null)

            verify { query.limit(20L) }
        }

        test("communityRawMentionCount가 있는 레포 DTO로 변환된다") {
            val entity = buildEntity(id = 1L)
            val community = GithubRepositoryCommunity(
                repoId = 1L,
                communityRawMentionCount = 5,
                communityCollectedAt = now,
            )
            val tuple = buildTuple(entity, community)
            mockTupleQuery(listOf(tuple))

            val result = repository.findForCommunityAnalyze(pageSize = 10, afterId = null)

            result[0].communityRawMentionCount shouldBe 5
        }
    }
})
