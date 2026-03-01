package com.techinsights.domain.repository.github

import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQuery
import com.querydsl.jpa.impl.JPAQueryFactory
import com.techinsights.domain.entity.github.GithubRepository
import com.techinsights.domain.entity.github.QGithubRepository
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

        test("STARS 정렬, language 필터 없이 조회하면 전체 결과를 반환한다") {
            val pageable = PageRequest.of(0, 20)
            val query = mockk<JPAQuery<GithubRepository>>()
            val countQuery = mockk<JPAQuery<Long>>()

            every { queryFactory.selectFrom(QGithubRepository.githubRepository) } returns query
            every { query.where(isNull<BooleanExpression>()) } returns query
            every { query.orderBy(any()) } returns query
            every { query.offset(0L) } returns query
            every { query.limit(20L) } returns query
            every { query.fetch() } returns listOf(entity1, entity2)

            every { queryFactory.select(QGithubRepository.githubRepository.id.count()) } returns countQuery
            every { countQuery.from(QGithubRepository.githubRepository) } returns countQuery
            every { countQuery.where(isNull<BooleanExpression>()) } returns countQuery
            every { countQuery.fetchOne() } returns 2L

            val result = repository.findRepositories(pageable, GithubSortType.STARS, null)

            result.content shouldHaveSize 2
            result.totalElements shouldBe 2L
        }

        test("language 필터를 적용하면 해당 언어 레포만 반환한다") {
            val pageable = PageRequest.of(0, 20)
            val query = mockk<JPAQuery<GithubRepository>>()
            val countQuery = mockk<JPAQuery<Long>>()

            every { queryFactory.selectFrom(QGithubRepository.githubRepository) } returns query
            every { query.where(any<BooleanExpression>()) } returns query
            every { query.orderBy(any()) } returns query
            every { query.offset(0L) } returns query
            every { query.limit(20L) } returns query
            every { query.fetch() } returns listOf(entity1)

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
            val query = mockk<JPAQuery<GithubRepository>>()
            val countQuery = mockk<JPAQuery<Long>>()

            every { queryFactory.selectFrom(QGithubRepository.githubRepository) } returns query
            every { query.where(isNull<BooleanExpression>()) } returns query
            every { query.orderBy(any()) } returns query
            every { query.offset(0L) } returns query
            every { query.limit(10L) } returns query
            every { query.fetch() } returns listOf(entity1)

            every { queryFactory.select(QGithubRepository.githubRepository.id.count()) } returns countQuery
            every { countQuery.from(QGithubRepository.githubRepository) } returns countQuery
            every { countQuery.where(isNull<BooleanExpression>()) } returns countQuery
            every { countQuery.fetchOne() } returns 1L

            val result = repository.findRepositories(pageable, GithubSortType.LATEST, null)

            result.content shouldHaveSize 1
            verify(exactly = 1) { query.orderBy(any()) }
        }

        test("TRENDING 정렬로 조회하면 orderBy가 호출된다") {
            val pageable = PageRequest.of(0, 10)
            val query = mockk<JPAQuery<GithubRepository>>()
            val countQuery = mockk<JPAQuery<Long>>()

            every { queryFactory.selectFrom(QGithubRepository.githubRepository) } returns query
            every { query.where(isNull<BooleanExpression>()) } returns query
            every { query.orderBy(any()) } returns query
            every { query.offset(0L) } returns query
            every { query.limit(10L) } returns query
            every { query.fetch() } returns listOf(entity1)

            every { queryFactory.select(QGithubRepository.githubRepository.id.count()) } returns countQuery
            every { countQuery.from(QGithubRepository.githubRepository) } returns countQuery
            every { countQuery.where(isNull<BooleanExpression>()) } returns countQuery
            every { countQuery.fetchOne() } returns 1L

            val result = repository.findRepositories(pageable, GithubSortType.TRENDING, null)

            result.content shouldHaveSize 1
            verify(exactly = 1) { query.orderBy(any()) }
        }

        test("결과가 없으면 totalElements=0인 빈 Page를 반환한다") {
            val pageable = PageRequest.of(0, 20)
            val query = mockk<JPAQuery<GithubRepository>>()
            val countQuery = mockk<JPAQuery<Long>>()

            every { queryFactory.selectFrom(QGithubRepository.githubRepository) } returns query
            every { query.where(isNull<BooleanExpression>()) } returns query
            every { query.orderBy(any()) } returns query
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
            val query = mockk<JPAQuery<GithubRepository>>()
            val countQuery = mockk<JPAQuery<Long>>()

            every { queryFactory.selectFrom(QGithubRepository.githubRepository) } returns query
            every { query.where(isNull<BooleanExpression>()) } returns query
            every { query.orderBy(any()) } returns query
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

        test("존재하는 id로 조회하면 DTO를 반환한다") {
            val query = mockk<JPAQuery<GithubRepository>>()

            every { queryFactory.selectFrom(QGithubRepository.githubRepository) } returns query
            every { query.where(any<BooleanExpression>()) } returns query
            every { query.fetchOne() } returns entity1

            val result = repository.findById(1L)

            result.shouldNotBeNull()
            result!!.fullName shouldBe "owner/repo1"
            result.starCount shouldBe 2000L
            result.primaryLanguage shouldBe "Kotlin"
        }

        test("존재하지 않는 id로 조회하면 null을 반환한다") {
            val query = mockk<JPAQuery<GithubRepository>>()

            every { queryFactory.selectFrom(QGithubRepository.githubRepository) } returns query
            every { query.where(any<BooleanExpression>()) } returns query
            every { query.fetchOne() } returns null

            val result = repository.findById(999L)

            result.shouldBeNull()
        }
    }


    context("findUnsummarized") {

        test("cursor 없이 호출하면 readmeSummarizedAt=null인 레포를 반환한다") {
            val query = mockk<JPAQuery<GithubRepository>>()

            every { queryFactory.selectFrom(QGithubRepository.githubRepository) } returns query
            every { query.where(any<BooleanExpression>(), isNull<BooleanExpression>()) } returns query
            every { query.orderBy(any(), any()) } returns query
            every { query.limit(10L) } returns query
            every { query.fetch() } returns listOf(entity1, entity2)

            val result = repository.findUnsummarized(pageSize = 10, afterStarCount = null, afterId = null)

            result shouldHaveSize 2
        }

        test("cursor를 지정하면 해당 cursor 이후 결과를 반환한다") {
            val query = mockk<JPAQuery<GithubRepository>>()

            every { queryFactory.selectFrom(QGithubRepository.githubRepository) } returns query
            every { query.where(any<BooleanExpression>(), any<BooleanExpression>()) } returns query
            every { query.orderBy(any(), any()) } returns query
            every { query.limit(10L) } returns query
            every { query.fetch() } returns listOf(entity2)

            val result = repository.findUnsummarized(pageSize = 10, afterStarCount = 2000L, afterId = 1L)

            result shouldHaveSize 1
            result[0].fullName shouldBe "owner/repo2"
            result[0].starCount shouldBe 1000L
        }

        test("결과가 없으면 빈 리스트를 반환한다") {
            val query = mockk<JPAQuery<GithubRepository>>()

            every { queryFactory.selectFrom(QGithubRepository.githubRepository) } returns query
            every { query.where(any<BooleanExpression>(), isNull<BooleanExpression>()) } returns query
            every { query.orderBy(any(), any()) } returns query
            every { query.limit(10L) } returns query
            every { query.fetch() } returns emptyList()

            val result = repository.findUnsummarized(pageSize = 10, afterStarCount = null, afterId = null)

            result.shouldBeEmpty()
        }

        test("pageSize가 limit()에 정확히 전달된다") {
            val query = mockk<JPAQuery<GithubRepository>>()

            every { queryFactory.selectFrom(QGithubRepository.githubRepository) } returns query
            every { query.where(any<BooleanExpression>(), isNull<BooleanExpression>()) } returns query
            every { query.orderBy(any(), any()) } returns query
            every { query.limit(5L) } returns query
            every { query.fetch() } returns emptyList()

            repository.findUnsummarized(pageSize = 5, afterStarCount = null, afterId = null)

            verify { query.limit(5L) }
        }

        test("topics 쉼표 구분 문자열이 List로 올바르게 변환된다") {
            val entityWithTopics = buildEntity(id = 3L, fullName = "owner/repo3", topics = "kotlin,spring,batch")
            val query = mockk<JPAQuery<GithubRepository>>()

            every { queryFactory.selectFrom(QGithubRepository.githubRepository) } returns query
            every { query.where(any<BooleanExpression>(), isNull<BooleanExpression>()) } returns query
            every { query.orderBy(any(), any()) } returns query
            every { query.limit(10L) } returns query
            every { query.fetch() } returns listOf(entityWithTopics)

            val result = repository.findUnsummarized(pageSize = 10, afterStarCount = null, afterId = null)

            result[0].topics shouldBe listOf("kotlin", "spring", "batch")
        }
    }
})


private fun buildEntity(
    id: Long,
    fullName: String = "owner/repo",
    starCount: Long = 1000L,
    language: String? = "Kotlin",
    weeklyDelta: Long = 0L,
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
)
