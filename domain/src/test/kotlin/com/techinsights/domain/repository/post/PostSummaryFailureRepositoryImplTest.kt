package com.techinsights.domain.repository.post

import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQuery
import com.querydsl.jpa.impl.JPAQueryFactory
import com.techinsights.domain.dto.failure.ErrorTypeStatDto
import com.techinsights.domain.entity.post.PostSummaryFailure
import com.techinsights.domain.entity.post.QPostSummaryFailure
import com.techinsights.domain.enums.SummaryErrorType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import java.time.LocalDateTime

class PostSummaryFailureRepositoryImplTest : FunSpec({

    val jpaRepository = mockk<PostSummaryFailureJpaRepository>()
    val queryFactory = mockk<JPAQueryFactory>()
    val repository = PostSummaryFailureRepositoryImpl(jpaRepository, queryFactory)
    val failureQ = QPostSummaryFailure.postSummaryFailure

    beforeTest {
        clearAllMocks()
    }

    test("save should delegate to jpaRepository") {
        val failure = mockk<PostSummaryFailure>()
        every { jpaRepository.save(failure) } returns failure

        repository.save(failure) shouldBe failure
        verify(exactly = 1) { jpaRepository.save(failure) }
    }

    test("findLatestByPostId should delegate to jpaRepository") {
        val failure = mockk<PostSummaryFailure>()
        every { jpaRepository.findTop1ByPostIdOrderByFailedAtDesc(1L) } returns failure

        repository.findLatestByPostId(1L) shouldBe failure
        verify(exactly = 1) { jpaRepository.findTop1ByPostIdOrderByFailedAtDesc(1L) }
    }

    test("findPostIdsWithErrorTypes should execute query correctly") {
        val query = mockk<JPAQuery<Long>>(relaxed = true)
        every { queryFactory.select(failureQ.postId) } returns query
        every { query.from(failureQ) } returns query
        every { query.where(any<com.querydsl.core.types.Predicate>()) } returns query
        every { query.distinct() } returns query
        every { query.fetch() } returns listOf(1L, 2L)

        val result = repository.findPostIdsWithErrorTypes(listOf(SummaryErrorType.TIMEOUT))

        result shouldBe listOf(1L, 2L)
        verify { query.where(any<com.querydsl.core.types.Predicate>()) }
    }

    test("getErrorTypeStatisticsSince should execute aggregation query") {
        val query = mockk<JPAQuery<ErrorTypeStatDto>>(relaxed = true)
        val stats = listOf(ErrorTypeStatDto(SummaryErrorType.TIMEOUT, 5))
        
        every { 
            queryFactory.select(any<com.querydsl.core.types.Expression<ErrorTypeStatDto>>()) 
        } returns query
        every { query.from(failureQ) } returns query
        every { query.where(any<com.querydsl.core.types.Predicate>()) } returns query
        every { query.groupBy(any<com.querydsl.core.types.Expression<*>>()) } returns query
        every { query.fetch() } returns stats

        val result = repository.getErrorTypeStatisticsSince(LocalDateTime.now())

        result shouldBe stats
    }
})
