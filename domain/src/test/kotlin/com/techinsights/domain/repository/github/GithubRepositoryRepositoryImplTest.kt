package com.techinsights.domain.repository.github

import com.querydsl.core.Tuple
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQuery
import com.querydsl.jpa.impl.JPAQueryFactory
import com.techinsights.domain.entity.github.GithubRepository
import com.techinsights.domain.entity.github.QGithubRepositoryCommunity
import com.techinsights.domain.entity.github.GithubRepositoryReadme
import com.techinsights.domain.entity.github.QGithubRepository
import com.techinsights.domain.entity.github.QGithubRepositoryReadme
import com.techinsights.domain.enums.ErrorType
import com.techinsights.domain.enums.GithubSortType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime

class GithubRepositoryRepositoryImplTest : FunSpec({

    val jpaRepository = mockk<GithubRepositoryJpaRepository>()
    val queryFactory = mockk<JPAQueryFactory>()
    val repository = GithubRepositoryRepositoryImpl(jpaRepository, queryFactory)

    val entity1 = buildEntity(id = 1L, fullName = "owner/repo1", starCount = 2000L, language = "Kotlin", weeklyDelta = 100L)
    val entity2 = buildEntity(id = 2L, fullName = "owner/repo2", starCount = 1000L, language = "Java", weeklyDelta = 50L)

    beforeTest { clearAllMocks() }

    context("findRepositories") {

        test("STARS 정렬, language 필터 없이 조회하면 readme 정보를 포함하여 반환한다") {
            val pageable = PageRequest.of(0, 20)
            val query = mockk<JPAQuery<Tuple>>()
            val countQuery = mockk<JPAQuery<Long>>()
            val tuple1 = mockk<Tuple>()
            val tuple2 = mockk<Tuple>()
            val readme1 = GithubRepositoryReadme(repoId = 1L, readmeSummary = "summary 1")

            every { queryFactory.select(QGithubRepository.githubRepository, QGithubRepositoryReadme.githubRepositoryReadme) } returns query
            every { query.from(QGithubRepository.githubRepository) } returns query
            every { query.leftJoin(QGithubRepositoryReadme.githubRepositoryReadme) } returns query
            every { query.on(any<BooleanExpression>()) } returns query
            every { query.where(isNull<BooleanExpression>()) } returns query
            every { query.orderBy(any(), any()) } returns query
            every { query.offset(0L) } returns query
            every { query.limit(20L) } returns query
            every { query.fetch() } returns listOf(tuple1, tuple2)

            every { tuple1.get(QGithubRepository.githubRepository) } returns entity1
            every { tuple1.get(QGithubRepositoryReadme.githubRepositoryReadme) } returns readme1
            every { tuple2.get(QGithubRepository.githubRepository) } returns entity2
            every { tuple2.get(QGithubRepositoryReadme.githubRepositoryReadme) } returns null

            every { queryFactory.select(QGithubRepository.githubRepository.id.count()) } returns countQuery
            every { countQuery.from(QGithubRepository.githubRepository) } returns countQuery
            every { countQuery.where(isNull<BooleanExpression>()) } returns countQuery
            every { countQuery.fetchOne() } returns 2L

            val result = repository.findRepositories(pageable, GithubSortType.STARS, null)

            result.content shouldHaveSize 2
            result.content[0].readmeSummary shouldBe "summary 1"
            result.content[1].readmeSummary.shouldBeNull()
            result.totalElements shouldBe 2L
        }

        test("language 필터를 적용하면 해당 언어 레포만 반환한다") {
            val pageable = PageRequest.of(0, 20)
            val query = mockk<JPAQuery<Tuple>>()
            val countQuery = mockk<JPAQuery<Long>>()
            val tuple1 = mockk<Tuple>()

            every { queryFactory.select(QGithubRepository.githubRepository, QGithubRepositoryReadme.githubRepositoryReadme) } returns query
            every { query.from(QGithubRepository.githubRepository) } returns query
            every { query.leftJoin(QGithubRepositoryReadme.githubRepositoryReadme) } returns query
            every { query.on(any<BooleanExpression>()) } returns query
            every { query.where(any<BooleanExpression>()) } returns query
            every { query.orderBy(any(), any()) } returns query
            every { query.offset(0L) } returns query
            every { query.limit(20L) } returns query
            every { query.fetch() } returns listOf(tuple1)

            every { tuple1.get(QGithubRepository.githubRepository) } returns entity1
            every { tuple1.get(QGithubRepositoryReadme.githubRepositoryReadme) } returns null

            every { queryFactory.select(QGithubRepository.githubRepository.id.count()) } returns countQuery
            every { countQuery.from(QGithubRepository.githubRepository) } returns countQuery
            every { countQuery.where(any<BooleanExpression>()) } returns countQuery
            every { countQuery.fetchOne() } returns 1L

            val result = repository.findRepositories(pageable, GithubSortType.STARS, "Kotlin")

            result.content shouldHaveSize 1
            result.content[0].primaryLanguage shouldBe "Kotlin"
            result.totalElements shouldBe 1L
        }

        test("LATEST 정렬로 조회하면 orderBy가 호출된다") {
            val pageable = PageRequest.of(0, 10)
            val query = mockk<JPAQuery<Tuple>>()
            val countQuery = mockk<JPAQuery<Long>>()
            val tuple1 = mockk<Tuple>()

            every { queryFactory.select(QGithubRepository.githubRepository, QGithubRepositoryReadme.githubRepositoryReadme) } returns query
            every { query.from(QGithubRepository.githubRepository) } returns query
            every { query.leftJoin(QGithubRepositoryReadme.githubRepositoryReadme) } returns query
            every { query.on(any<BooleanExpression>()) } returns query
            every { query.where(isNull<BooleanExpression>()) } returns query
            every { query.orderBy(any(), any()) } returns query
            every { query.offset(0L) } returns query
            every { query.limit(10L) } returns query
            every { query.fetch() } returns listOf(tuple1)

            every { tuple1.get(QGithubRepository.githubRepository) } returns entity1
            every { tuple1.get(QGithubRepositoryReadme.githubRepositoryReadme) } returns null

            every { queryFactory.select(QGithubRepository.githubRepository.id.count()) } returns countQuery
            every { countQuery.from(QGithubRepository.githubRepository) } returns countQuery
            every { countQuery.where(isNull<BooleanExpression>()) } returns countQuery
            every { countQuery.fetchOne() } returns 1L

            val result = repository.findRepositories(pageable, GithubSortType.LATEST, null)

            result.content shouldHaveSize 1
            verify(exactly = 1) { query.orderBy(any(), any()) }
        }

        test("TRENDING 정렬로 조회하면 orderBy가 호출된다") {
            val pageable = PageRequest.of(0, 10)
            val query = mockk<JPAQuery<Tuple>>()
            val countQuery = mockk<JPAQuery<Long>>()
            val tuple1 = mockk<Tuple>()

            every { queryFactory.select(QGithubRepository.githubRepository, QGithubRepositoryReadme.githubRepositoryReadme) } returns query
            every { query.from(QGithubRepository.githubRepository) } returns query
            every { query.leftJoin(QGithubRepositoryReadme.githubRepositoryReadme) } returns query
            every { query.on(any<BooleanExpression>()) } returns query
            every { query.where(isNull<BooleanExpression>()) } returns query
            every { query.orderBy(any(), any(), any()) } returns query
            every { query.offset(0L) } returns query
            every { query.limit(10L) } returns query
            every { query.fetch() } returns listOf(tuple1)

            every { tuple1.get(QGithubRepository.githubRepository) } returns entity1
            every { tuple1.get(QGithubRepositoryReadme.githubRepositoryReadme) } returns null

            every { queryFactory.select(QGithubRepository.githubRepository.id.count()) } returns countQuery
            every { countQuery.from(QGithubRepository.githubRepository) } returns countQuery
            every { countQuery.where(isNull<BooleanExpression>()) } returns countQuery
            every { countQuery.fetchOne() } returns 1L

            val result = repository.findRepositories(pageable, GithubSortType.TRENDING, null)

            result.content shouldHaveSize 1
            verify(exactly = 1) { query.orderBy(any(), any(), any()) }
        }

        test("DAILY_TRENDING 정렬로 조회하면 daily_star_delta DESC 순으로 반환된다") {
            val pageable = PageRequest.of(0, 10)
            val query = mockk<JPAQuery<Tuple>>()
            val countQuery = mockk<JPAQuery<Long>>()
            val tuple1 = mockk<Tuple>()
            val tuple2 = mockk<Tuple>()

            val highDailyEntity = buildEntity(id = 1L, fullName = "owner/repo1", dailyDelta = 100L)
            val lowDailyEntity = buildEntity(id = 2L, fullName = "owner/repo2", dailyDelta = 10L)

            every { queryFactory.select(QGithubRepository.githubRepository, QGithubRepositoryReadme.githubRepositoryReadme) } returns query
            every { query.from(QGithubRepository.githubRepository) } returns query
            every { query.leftJoin(QGithubRepositoryReadme.githubRepositoryReadme) } returns query
            every { query.on(any<BooleanExpression>()) } returns query
            every { query.where(isNull<BooleanExpression>()) } returns query
            every { query.orderBy(any(), any(), any()) } returns query
            every { query.offset(0L) } returns query
            every { query.limit(10L) } returns query
            every { query.fetch() } returns listOf(tuple1, tuple2)

            every { tuple1.get(QGithubRepository.githubRepository) } returns highDailyEntity
            every { tuple1.get(QGithubRepositoryReadme.githubRepositoryReadme) } returns null
            every { tuple2.get(QGithubRepository.githubRepository) } returns lowDailyEntity
            every { tuple2.get(QGithubRepositoryReadme.githubRepositoryReadme) } returns null

            every { queryFactory.select(QGithubRepository.githubRepository.id.count()) } returns countQuery
            every { countQuery.from(QGithubRepository.githubRepository) } returns countQuery
            every { countQuery.where(isNull<BooleanExpression>()) } returns countQuery
            every { countQuery.fetchOne() } returns 2L

            val result = repository.findRepositories(pageable, GithubSortType.DAILY_TRENDING, null)

            result.content shouldHaveSize 2
            result.content[0].dailyStarDelta shouldBe 100L
            result.content[1].dailyStarDelta shouldBe 10L
            verify(exactly = 1) { query.orderBy(any(), any(), any()) }
        }

        test("결과가 없으면 totalElements=0인 빈 Page를 반환한다") {
            val pageable = PageRequest.of(0, 20)
            val query = mockk<JPAQuery<Tuple>>()
            val countQuery = mockk<JPAQuery<Long>>()

            every { queryFactory.select(QGithubRepository.githubRepository, QGithubRepositoryReadme.githubRepositoryReadme) } returns query
            every { query.from(QGithubRepository.githubRepository) } returns query
            every { query.leftJoin(QGithubRepositoryReadme.githubRepositoryReadme) } returns query
            every { query.on(any<BooleanExpression>()) } returns query
            every { query.where(isNull<BooleanExpression>()) } returns query
            every { query.orderBy(any(), any()) } returns query
            every { query.offset(0L) } returns query
            every { query.limit(20L) } returns query
            every { query.fetch() } returns emptyList()

            every { queryFactory.select(QGithubRepository.githubRepository.id.count()) } returns countQuery
            every { countQuery.from(QGithubRepository.githubRepository) } returns countQuery
            every { countQuery.where(isNull<BooleanExpression>()) } returns countQuery
            every { countQuery.fetchOne() } returns null   // null → 0L

            val result = repository.findRepositories(pageable, GithubSortType.STARS, null)

            result.content.shouldBeEmpty()
            result.totalElements shouldBe 0L
        }

        test("pageable의 offset과 limit이 쿼리에 올바르게 전달된다") {
            val pageable = PageRequest.of(2, 5)   // offset = 10, limit = 5
            val query = mockk<JPAQuery<Tuple>>()
            val countQuery = mockk<JPAQuery<Long>>()

            every { queryFactory.select(QGithubRepository.githubRepository, QGithubRepositoryReadme.githubRepositoryReadme) } returns query
            every { query.from(QGithubRepository.githubRepository) } returns query
            every { query.leftJoin(QGithubRepositoryReadme.githubRepositoryReadme) } returns query
            every { query.on(any<BooleanExpression>()) } returns query
            every { query.where(isNull<BooleanExpression>()) } returns query
            every { query.orderBy(any(), any()) } returns query
            every { query.offset(10L) } returns query
            every { query.limit(5L) } returns query
            every { query.fetch() } returns emptyList()

            every { queryFactory.select(QGithubRepository.githubRepository.id.count()) } returns countQuery
            every { countQuery.from(QGithubRepository.githubRepository) } returns countQuery
            every { countQuery.where(isNull<BooleanExpression>()) } returns countQuery
            every { countQuery.fetchOne() } returns 0L

            repository.findRepositories(pageable, GithubSortType.STARS, null)

            verify { query.offset(10L) }
            verify { query.limit(5L) }
        }
    }

    context("findById") {

        test("존재하는 id로 조회하면 readme 정보를 포함하여 DTO를 반환한다") {
            val repo = QGithubRepository.githubRepository
            val community = QGithubRepositoryCommunity.githubRepositoryCommunity
            val readme = QGithubRepositoryReadme.githubRepositoryReadme
            val query = mockk<JPAQuery<Tuple>>()
            val tuple = mockk<Tuple>()

            every { queryFactory.select(repo, community, readme) } returns query
            every { query.from(repo) } returns query
            every { query.leftJoin(community) } returns query
            every { query.on(any<BooleanExpression>()) } returns query
            every { query.leftJoin(readme) } returns query
            every { query.on(any<BooleanExpression>()) } returns query
            every { query.where(any<BooleanExpression>()) } returns query
            every { query.fetchOne() } returns tuple
            every { tuple.get(repo) } returns entity1
            every { tuple.get(community) } returns null
            every { tuple.get(readme) } returns GithubRepositoryReadme(repoId = 1L, readmeSummary = "detailed summary")

            val result = repository.findById(1L)

            result.shouldNotBeNull()
            result.readmeSummary shouldBe "detailed summary"
            result.fullName shouldBe "owner/repo1"
        }

        test("존재하지 않는 id로 조회하면 null을 반환한다") {
            val repo = QGithubRepository.githubRepository
            val community = QGithubRepositoryCommunity.githubRepositoryCommunity
            val readme = QGithubRepositoryReadme.githubRepositoryReadme
            val query = mockk<JPAQuery<Tuple>>()

            every { queryFactory.select(repo, community, readme) } returns query
            every { query.from(repo) } returns query
            every { query.leftJoin(community) } returns query
            every { query.on(any<BooleanExpression>()) } returns query
            every { query.leftJoin(readme) } returns query
            every { query.on(any<BooleanExpression>()) } returns query
            every { query.where(any<BooleanExpression>()) } returns query
            every { query.fetchOne() } returns null

            val result = repository.findById(999L)

            result.shouldBeNull()
        }
    }


    context("findUnsummarized") {

        test("cursor 없이 호출하면 readmeSummarizedAt=null인 레포를 반환한다") {
            val query = mockk<JPAQuery<Tuple>>()
            val tuple1 = mockk<Tuple>()
            val tuple2 = mockk<Tuple>()

            every { queryFactory.select(QGithubRepository.githubRepository, QGithubRepositoryReadme.githubRepositoryReadme) } returns query
            every { query.from(QGithubRepository.githubRepository) } returns query
            every { query.leftJoin(QGithubRepositoryReadme.githubRepositoryReadme) } returns query
            every { query.on(any<BooleanExpression>()) } returns query
            every { query.where(any<BooleanExpression>(), isNull<BooleanExpression>()) } returns query
            every { query.orderBy(any(), any()) } returns query
            every { query.limit(10L) } returns query
            every { query.fetch() } returns listOf(tuple1, tuple2)

            every { tuple1.get(QGithubRepository.githubRepository) } returns entity1
            every { tuple2.get(QGithubRepository.githubRepository) } returns entity2

            val result = repository.findUnsummarized(pageSize = 10, afterStarCount = null, afterId = null)

            result shouldHaveSize 2
        }

        test("cursor를 지정하면 해당 cursor 이후 결과를 반환한다") {
            val query = mockk<JPAQuery<Tuple>>()
            val tuple2 = mockk<Tuple>()

            every { queryFactory.select(QGithubRepository.githubRepository, QGithubRepositoryReadme.githubRepositoryReadme) } returns query
            every { query.from(QGithubRepository.githubRepository) } returns query
            every { query.leftJoin(QGithubRepositoryReadme.githubRepositoryReadme) } returns query
            every { query.on(any<BooleanExpression>()) } returns query
            every { query.where(any<BooleanExpression>(), any<BooleanExpression>()) } returns query
            every { query.orderBy(any(), any()) } returns query
            every { query.limit(10L) } returns query
            every { query.fetch() } returns listOf(tuple2)

            every { tuple2.get(QGithubRepository.githubRepository) } returns entity2

            val result = repository.findUnsummarized(pageSize = 10, afterStarCount = 2000L, afterId = 1L)

            result shouldHaveSize 1
            result[0].fullName shouldBe "owner/repo2"
            result[0].starCount shouldBe 1000L
        }

        test("결과가 없으면 빈 리스트를 반환한다") {
            val query = mockk<JPAQuery<Tuple>>()

            every { queryFactory.select(QGithubRepository.githubRepository, QGithubRepositoryReadme.githubRepositoryReadme) } returns query
            every { query.from(QGithubRepository.githubRepository) } returns query
            every { query.leftJoin(QGithubRepositoryReadme.githubRepositoryReadme) } returns query
            every { query.on(any<BooleanExpression>()) } returns query
            every { query.where(any<BooleanExpression>(), isNull<BooleanExpression>()) } returns query
            every { query.orderBy(any(), any()) } returns query
            every { query.limit(10L) } returns query
            every { query.fetch() } returns emptyList()

            val result = repository.findUnsummarized(pageSize = 10, afterStarCount = null, afterId = null)

            result.shouldBeEmpty()
        }

        test("pageSize가 limit()에 정확히 전달된다") {
            val query = mockk<JPAQuery<Tuple>>()

            every { queryFactory.select(QGithubRepository.githubRepository, QGithubRepositoryReadme.githubRepositoryReadme) } returns query
            every { query.from(QGithubRepository.githubRepository) } returns query
            every { query.leftJoin(QGithubRepositoryReadme.githubRepositoryReadme) } returns query
            every { query.on(any<BooleanExpression>()) } returns query
            every { query.where(any<BooleanExpression>(), isNull<BooleanExpression>()) } returns query
            every { query.orderBy(any(), any()) } returns query
            every { query.limit(5L) } returns query
            every { query.fetch() } returns emptyList()

            repository.findUnsummarized(pageSize = 5, afterStarCount = null, afterId = null)

            verify { query.limit(5L) }
        }

        test("topics 쉼표 구분 문자열이 List로 올바르게 변환된다") {
            val entityWithTopics = buildEntity(id = 3L, fullName = "owner/repo3", topics = "kotlin,spring,batch")
            val query = mockk<JPAQuery<Tuple>>()
            val tuple3 = mockk<Tuple>()

            every { queryFactory.select(QGithubRepository.githubRepository, QGithubRepositoryReadme.githubRepositoryReadme) } returns query
            every { query.from(QGithubRepository.githubRepository) } returns query
            every { query.leftJoin(QGithubRepositoryReadme.githubRepositoryReadme) } returns query
            every { query.on(any<BooleanExpression>()) } returns query
            every { query.where(any<BooleanExpression>(), isNull<BooleanExpression>()) } returns query
            every { query.orderBy(any(), any()) } returns query
            every { query.limit(10L) } returns query
            every { query.fetch() } returns listOf(tuple3)

            every { tuple3.get(QGithubRepository.githubRepository) } returns entityWithTopics

            val result = repository.findUnsummarized(pageSize = 10, afterStarCount = null, afterId = null)

            result[0].topics shouldBe listOf("kotlin", "spring", "batch")
        }

        test("retryAfter와 retryableErrorTypes 설정 시 결과를 반환한다") {
            val query = mockk<JPAQuery<Tuple>>()
            val tuple1 = mockk<Tuple>()

            every { queryFactory.select(QGithubRepository.githubRepository, QGithubRepositoryReadme.githubRepositoryReadme) } returns query
            every { query.from(QGithubRepository.githubRepository) } returns query
            every { query.leftJoin(QGithubRepositoryReadme.githubRepositoryReadme) } returns query
            every { query.on(any<BooleanExpression>()) } returns query
            every { query.where(any<BooleanExpression>(), isNull<BooleanExpression>()) } returns query
            every { query.orderBy(any(), any()) } returns query
            every { query.limit(10L) } returns query
            every { query.fetch() } returns listOf(tuple1)

            every { tuple1.get(QGithubRepository.githubRepository) } returns entity1

            val result = repository.findUnsummarized(
                pageSize = 10,
                afterStarCount = null,
                afterId = null,
                retryAfter = LocalDateTime.now().minusDays(7),
                retryableErrorTypes = setOf(ErrorType.LENGTH_LIMIT, ErrorType.API_ERROR),
            )

            result shouldHaveSize 1
        }

        test("retryableErrorTypes가 빈 Set이면 retryAfter가 있어도 neverAttempted 조건만 사용한다") {
            val query = mockk<JPAQuery<Tuple>>()
            val tuple1 = mockk<Tuple>()

            every { queryFactory.select(QGithubRepository.githubRepository, QGithubRepositoryReadme.githubRepositoryReadme) } returns query
            every { query.from(QGithubRepository.githubRepository) } returns query
            every { query.leftJoin(QGithubRepositoryReadme.githubRepositoryReadme) } returns query
            every { query.on(any<BooleanExpression>()) } returns query
            every { query.where(any<BooleanExpression>(), isNull<BooleanExpression>()) } returns query
            every { query.orderBy(any(), any()) } returns query
            every { query.limit(10L) } returns query
            every { query.fetch() } returns listOf(tuple1)

            every { tuple1.get(QGithubRepository.githubRepository) } returns entity1

            val result = repository.findUnsummarized(
                pageSize = 10,
                afterStarCount = null,
                afterId = null,
                retryAfter = LocalDateTime.now().minusDays(7),
                retryableErrorTypes = emptySet(),
            )

            result shouldHaveSize 1
        }
    }

    context("findUnembedded") {

        test("cursor 없이 호출하면 요약은 됐지만 아직 임베딩 안 된 레포를 반환한다") {
            val tupleQuery = mockk<JPAQuery<Tuple>>()
            val readme1 = GithubRepositoryReadme(repoId = 1L, readmeSummary = "summary1")
            val readme2 = GithubRepositoryReadme(repoId = 2L, readmeSummary = "summary2")
            val tuple1 = mockk<Tuple>()
            val tuple2 = mockk<Tuple>()

            val repo = QGithubRepository.githubRepository
            val readme = QGithubRepositoryReadme.githubRepositoryReadme

            every { queryFactory.select(repo, readme) } returns tupleQuery
            every { tupleQuery.from(repo) } returns tupleQuery
            every { tupleQuery.join(readme) } returns tupleQuery
            every { tupleQuery.on(any<BooleanExpression>()) } returns tupleQuery
            every { tupleQuery.where(any(), any(), any(), isNull()) } returns tupleQuery
            every { tupleQuery.orderBy(any(), any()) } returns tupleQuery
            every { tupleQuery.limit(10L) } returns tupleQuery
            every { tuple1.get(repo) } returns entity1
            every { tuple1.get(readme) } returns readme1
            every { tuple2.get(repo) } returns entity2
            every { tuple2.get(readme) } returns readme2
            every { tupleQuery.fetch() } returns listOf(tuple1, tuple2)

            val result = repository.findUnembedded(pageSize = 10, afterStarCount = null, afterId = null)

            result shouldHaveSize 2
        }

        test("cursor를 지정하면 해당 cursor 이후 결과를 반환한다") {
            val tupleQuery = mockk<JPAQuery<Tuple>>()
            val readme2 = GithubRepositoryReadme(repoId = 2L, readmeSummary = "summary2")
            val tuple2 = mockk<Tuple>()

            val repo = QGithubRepository.githubRepository
            val readme = QGithubRepositoryReadme.githubRepositoryReadme

            every { queryFactory.select(repo, readme) } returns tupleQuery
            every { tupleQuery.from(repo) } returns tupleQuery
            every { tupleQuery.join(readme) } returns tupleQuery
            every { tupleQuery.on(any<BooleanExpression>()) } returns tupleQuery
            every { tupleQuery.where(any(), any(), any(), any<BooleanExpression>()) } returns tupleQuery
            every { tupleQuery.orderBy(any(), any()) } returns tupleQuery
            every { tupleQuery.limit(10L) } returns tupleQuery
            every { tuple2.get(repo) } returns entity2
            every { tuple2.get(readme) } returns readme2
            every { tupleQuery.fetch() } returns listOf(tuple2)

            val result = repository.findUnembedded(pageSize = 10, afterStarCount = 2000L, afterId = 1L)

            result shouldHaveSize 1
            result[0].fullName shouldBe "owner/repo2"
        }

        test("결과가 없으면 빈 리스트를 반환한다") {
            val tupleQuery = mockk<JPAQuery<Tuple>>()
            val repo = QGithubRepository.githubRepository
            val readme = QGithubRepositoryReadme.githubRepositoryReadme

            every { queryFactory.select(repo, readme) } returns tupleQuery
            every { tupleQuery.from(repo) } returns tupleQuery
            every { tupleQuery.join(readme) } returns tupleQuery
            every { tupleQuery.on(any<BooleanExpression>()) } returns tupleQuery
            every { tupleQuery.where(any(), any(), any(), isNull()) } returns tupleQuery
            every { tupleQuery.orderBy(any(), any()) } returns tupleQuery
            every { tupleQuery.limit(10L) } returns tupleQuery
            every { tupleQuery.fetch() } returns emptyList()

            val result = repository.findUnembedded(pageSize = 10, afterStarCount = null, afterId = null)

            result.shouldBeEmpty()
        }

        test("pageSize가 limit()에 정확히 전달된다") {
            val tupleQuery = mockk<JPAQuery<Tuple>>()
            val repo = QGithubRepository.githubRepository
            val readme = QGithubRepositoryReadme.githubRepositoryReadme

            every { queryFactory.select(repo, readme) } returns tupleQuery
            every { tupleQuery.from(repo) } returns tupleQuery
            every { tupleQuery.join(readme) } returns tupleQuery
            every { tupleQuery.on(any<BooleanExpression>()) } returns tupleQuery
            every { tupleQuery.where(any(), any(), any(), isNull()) } returns tupleQuery
            every { tupleQuery.orderBy(any(), any()) } returns tupleQuery
            every { tupleQuery.limit(7L) } returns tupleQuery
            every { tupleQuery.fetch() } returns emptyList()

            repository.findUnembedded(pageSize = 7, afterStarCount = null, afterId = null)

            verify { tupleQuery.limit(7L) }
        }
    }

    context("findRepositoriesByCursor") {
        test("cursor를 사용하여 레포지토리를 조회한다") {
            val repo = QGithubRepository.githubRepository
            val readme = QGithubRepositoryReadme.githubRepositoryReadme
            val query = mockk<JPAQuery<Tuple>>()
            val tuple1 = mockk<Tuple>()

            every { queryFactory.select(repo, readme) } returns query
            every { query.from(repo) } returns query
            every { query.leftJoin(readme) } returns query
            every { query.on(any<BooleanExpression>()) } returns query
            every { query.where(isNull<BooleanExpression>(), isNull<BooleanExpression>()) } returns query
            every { query.orderBy(any(), any()) } returns query
            every { query.limit(10L) } returns query
            every { query.fetch() } returns listOf(tuple1)
            every { tuple1.get(repo) } returns entity1
            every { tuple1.get(readme) } returns null

            val result = repository.findRepositoriesByCursor(10, GithubSortType.STARS, null, null)

            result shouldHaveSize 1
            result[0].id shouldBe 1L
        }
    }

    context("findForCommunityCollect") {
        test("커뮤니티 수집 대상 레포지토리를 조회한다") {
            val repo = QGithubRepository.githubRepository
            val community = QGithubRepositoryCommunity.githubRepositoryCommunity
            val query = mockk<JPAQuery<Tuple>>()
            val tuple1 = mockk<Tuple>()

            every { queryFactory.select(repo, community) } returns query
            every { query.from(repo) } returns query
            every { query.leftJoin(community) } returns query
            every { query.on(any<BooleanExpression>()) } returns query
            every { query.where(any<BooleanExpression>(), isNull<BooleanExpression>()) } returns query
            every { query.orderBy(any(), any()) } returns query
            every { query.limit(10L) } returns query
            every { query.fetch() } returns listOf(tuple1)
            every { tuple1.get(repo) } returns entity1
            every { tuple1.get(community) } returns null

            val result = repository.findForCommunityCollect(
                10, null, null,
                LocalDateTime.now(), LocalDateTime.now()
            )

            result shouldHaveSize 1
        }
    }

    context("findForCommunityAnalyze") {
        test("커뮤니티 분석 대상 레포지토리를 조회한다") {
            val repo = QGithubRepository.githubRepository
            val community = QGithubRepositoryCommunity.githubRepositoryCommunity
            val query = mockk<JPAQuery<Tuple>>()
            val tuple1 = mockk<Tuple>()

            every { queryFactory.select(repo, community) } returns query
            every { query.from(repo) } returns query
            every { query.join(community) } returns query
            every { query.on(any<BooleanExpression>()) } returns query
            every { query.where(any<BooleanExpression>(), isNull<BooleanExpression>()) } returns query
            every { query.orderBy(any()) } returns query
            every { query.limit(10L) } returns query
            every { query.fetch() } returns listOf(tuple1)
            every { tuple1.get(repo) } returns entity1
            every { tuple1.get(community) } returns null

            val result = repository.findForCommunityAnalyze(10, null)

            result shouldHaveSize 1
        }
    }

    context("findSimilarRepositories") {

        test("targetVector와 limit을 JpaRepository로 위임하여 결과를 반환한다") {
            val projection = mockk<GithubRepositoryWithDistance> {
                every { fullName } returns "owner/repo1"
                every { repoName } returns "repo1"
                every { description } returns "desc"
                every { readmeSummary } returns "summary"
                every { primaryLanguage } returns "Kotlin"
                every { starCount } returns 1000L
                every { ownerName } returns "owner"
                every { ownerAvatarUrl } returns null
                every { topics } returns "kotlin,test"
                every { htmlUrl } returns "https://github.com/owner/repo1"
                every { distance } returns 0.15
            }
            every { jpaRepository.findSimilarRepositories("[0.1,0.2]", 5L) } returns listOf(projection)

            val result = repository.findSimilarRepositories("[0.1,0.2]", 5L)

            result shouldHaveSize 1
            result[0].fullName shouldBe "owner/repo1"
            result[0].distance shouldBe 0.15
        }

        test("결과가 없으면 빈 리스트를 반환한다") {
            every { jpaRepository.findSimilarRepositories(any(), any()) } returns emptyList()

            val result = repository.findSimilarRepositories("[0.1,0.2]", 5L)

            result.shouldBeEmpty()
        }
    }
})


private fun buildEntity(
    id: Long,
    fullName: String = "owner/repo",
    starCount: Long = 1000L,
    language: String? = "Kotlin",
    weeklyDelta: Long = 0L,
    dailyDelta: Long = 0L,
    topics: String? = "kotlin,test",
): GithubRepository = GithubRepository(
    id = id,
    repoName = fullName.substringAfter("/"),
    fullName = fullName,
    description = "Test repository description",
    htmlUrl = "https://github.com/$fullName",
    starCount = starCount,
    forkCount = 100L,
    primaryLanguage = language,
    ownerName = fullName.substringBefore("/"),
    ownerAvatarUrl = null,
    topics = topics,
    pushedAt = LocalDateTime.of(2024, 6, 1, 0, 0),
    fetchedAt = LocalDateTime.of(2024, 6, 2, 0, 0),
    weeklyStarDelta = weeklyDelta,
    dailyStarDelta = dailyDelta,
)
