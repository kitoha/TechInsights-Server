package com.techinsights.domain.repository.search

import com.querydsl.core.Tuple
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.core.types.dsl.NumberExpression
import com.querydsl.jpa.impl.JPAQuery
import com.querydsl.jpa.impl.JPAQueryFactory
import com.techinsights.domain.entity.company.QCompany
import com.techinsights.domain.entity.post.Post
import com.techinsights.domain.entity.post.QPost
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class PostSearchQueryRepositoryTest : FunSpec({

  val jpaQueryFactory = mockk<JPAQueryFactory>()
  val tupleQuery = mockk<JPAQuery<Tuple>>()
  val countQuery = mockk<JPAQuery<Long>>()

  val repository = PostSearchQueryRepository(jpaQueryFactory)

  val post = QPost.post
  val company = QCompany.company

  val mockPost = mockk<Post>()
  val mockScore = 0.95
  val mockTuple = mockk<Tuple>()

  beforeTest {
    clearMocks(jpaQueryFactory, tupleQuery, countQuery, mockTuple)

    every { mockTuple.get(post) } returns mockPost
  }

  test("findForInstantSearch should execute query and return projections") {
    val searchQuery = "test"
    val limit = 5
    val relevanceScore = mockk<NumberExpression<Double>>()
    val orderScore = mockk<OrderSpecifier<Double>>()

    every { relevanceScore.desc() } returns orderScore
    every { mockTuple.get(relevanceScore) } returns mockScore

    every { jpaQueryFactory.select(post, relevanceScore) } returns tupleQuery
    every { tupleQuery.from(post) } returns tupleQuery
    every { tupleQuery.join(post.company, company) } returns tupleQuery
    every { tupleQuery.fetchJoin() } returns tupleQuery
    every { tupleQuery.where(any<BooleanExpression>(), post.isSummary.isTrue) } returns tupleQuery
    every { tupleQuery.orderBy(orderScore, post.viewCount.desc()) } returns tupleQuery
    every { tupleQuery.limit(limit.toLong()) } returns tupleQuery
    every { tupleQuery.fetch() } returns listOf(mockTuple)

    val result = repository.findForInstantSearch(searchQuery, relevanceScore, limit)

    result.size shouldBe 1
    result[0].post shouldBe mockPost
    result[0].relevanceScore shouldBe mockScore

    verify {
      jpaQueryFactory.select(post, relevanceScore)
      tupleQuery.from(post)
      tupleQuery.join(post.company, company)
      tupleQuery.fetchJoin()
      tupleQuery.where(any<BooleanExpression>(), post.isSummary.isTrue)
      tupleQuery.orderBy(orderScore, post.viewCount.desc())
      tupleQuery.limit(5L)
      tupleQuery.fetch()
    }
  }

  test("findForFullSearch should execute paginated query and return projections") {
    val condition = mockk<BooleanExpression>()
    val relevanceScore = mockk<NumberExpression<Double>>()
    val orderSpecifier1 = mockk<OrderSpecifier<*>>()
    val orderSpecifiers = arrayOf(orderSpecifier1)
    val offset = 10L
    val limit = 20L

    every { jpaQueryFactory.select(post, relevanceScore) } returns tupleQuery
    every { tupleQuery.from(post) } returns tupleQuery
    every { tupleQuery.join(post.company, company) } returns tupleQuery
    every { tupleQuery.fetchJoin() } returns tupleQuery
    every { tupleQuery.where(condition, post.isSummary.isTrue) } returns tupleQuery
    every { tupleQuery.orderBy(*orderSpecifiers) } returns tupleQuery
    every { tupleQuery.offset(offset) } returns tupleQuery
    every { tupleQuery.limit(limit) } returns tupleQuery
    every { tupleQuery.fetch() } returns listOf(mockTuple, mockTuple)
    every { mockTuple.get(relevanceScore) } returns mockScore

    val result =
      repository.findForFullSearch(condition, orderSpecifiers, relevanceScore, offset, limit)

    result.size shouldBe 2
    result[0].post shouldBe mockPost
    result[1].relevanceScore shouldBe mockScore

    verify {
      jpaQueryFactory.select(post, relevanceScore)
      tupleQuery.where(condition, post.isSummary.isTrue)
      tupleQuery.orderBy(*orderSpecifiers)
      tupleQuery.offset(offset)
      tupleQuery.limit(limit)
      tupleQuery.fetch()
    }
  }

  test("countByCondition should execute count query and return result") {
    val condition = mockk<BooleanExpression>()
    val expectedCount = 7L

    every { jpaQueryFactory.select(post.count()) } returns countQuery
    every { countQuery.from(post) } returns countQuery
    every { countQuery.join(post.company, company) } returns countQuery
    every { countQuery.where(condition, post.isSummary.isTrue) } returns countQuery
    every { countQuery.fetchOne() } returns expectedCount

    val result = repository.countByCondition(condition)

    result shouldBe expectedCount

    verify {
      jpaQueryFactory.select(post.count())
      countQuery.from(post)
      countQuery.join(post.company, company)
      countQuery.where(condition, post.isSummary.isTrue)
      countQuery.fetchOne()
    }
  }

  test("countByCondition should return 0 when fetchOne is null") {
    val condition = mockk<BooleanExpression>()

    every { jpaQueryFactory.select(post.count()) } returns countQuery
    every { countQuery.from(post) } returns countQuery
    every { countQuery.join(post.company, company) } returns countQuery
    every { countQuery.where(condition, post.isSummary.isTrue) } returns countQuery
    every { countQuery.fetchOne() } returns null

    val result = repository.countByCondition(condition)

    result shouldBe 0L
  }

  test("countMatchedPostsByCompany should execute specific count query") {
    val companyId = 123L
    val query = "search term"
    val expectedCount = 3L

    every { jpaQueryFactory.select(post.count()) } returns countQuery
    every { countQuery.from(post) } returns countQuery
    every {
      countQuery.where(
        any<BooleanExpression>(),
        any<BooleanExpression>(),
        post.isSummary.isTrue
      )
    } returns countQuery
    every { countQuery.fetchOne() } returns expectedCount

    val result = repository.countMatchedPostsByCompany(companyId, query)

    result shouldBe expectedCount

    verify {
      jpaQueryFactory.select(post.count())
      countQuery.from(post)
      countQuery.where(
        post.company.id.eq(companyId),
        any<BooleanExpression>(),
        post.isSummary.isTrue
      )
      countQuery.fetchOne()
    }
  }
})
