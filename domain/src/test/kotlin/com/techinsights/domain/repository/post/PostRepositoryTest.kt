package com.techinsights.domain.repository.post

import com.querydsl.core.types.Path
import com.querydsl.core.types.dsl.NumberExpression
import com.querydsl.jpa.impl.JPAQuery
import com.querydsl.jpa.impl.JPAQueryFactory
import com.querydsl.jpa.impl.JPAUpdateClause
import com.techinsights.domain.entity.post.QPost
import com.techinsights.domain.exception.PostNotFoundException
import com.techinsights.domain.utils.Tsid
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk

class PostRepositoryTest : FunSpec({

    val postJpaRepository = mockk<PostJpaRepository>()
    val queryFactory = mockk<JPAQueryFactory>()
    val repository = PostRepositoryImpl(postJpaRepository, queryFactory)

    val longQuery = mockk<JPAQuery<Long>>()
    val updateClause = mockk<JPAUpdateClause>()
    val post = QPost.post

    beforeTest {
        clearMocks(queryFactory, longQuery, updateClause, postJpaRepository)
    }

    test("getLikeCount should return likeCount when post exists") {
        val postId = Tsid.generateLong()

        every { queryFactory.select(post.likeCount) } returns longQuery
        every { longQuery.from(post) } returns longQuery
        every { longQuery.where(any()) } returns longQuery
        every { longQuery.fetchOne() } returns 5L

        val result = repository.getLikeCount(postId)

        result shouldBe 5L
    }

    test("incrementLikeCount should succeed when post exists") {
        val postId = Tsid.generateLong()

        every { queryFactory.update(post) } returns updateClause
        every { updateClause.set(any<Path<Long>>(), any<NumberExpression<Long>>()) } returns updateClause
        every { updateClause.where(any()) } returns updateClause
        every { updateClause.execute() } returns 1L

        repository.incrementLikeCount(postId)
    }

    test("incrementLikeCount should throw PostNotFoundException when post not found") {
        val postId = Tsid.generateLong()

        every { queryFactory.update(post) } returns updateClause
        every { updateClause.set(any<Path<Long>>(), any<NumberExpression<Long>>()) } returns updateClause
        every { updateClause.where(any()) } returns updateClause
        every { updateClause.execute() } returns 0L

        shouldThrow<PostNotFoundException> {
            repository.incrementLikeCount(postId)
        }
    }

    test("decrementLikeCount should succeed when post exists and likeCount is positive") {
        val postId = Tsid.generateLong()

        every { queryFactory.update(post) } returns updateClause
        every { updateClause.set(any<Path<Long>>(), any<NumberExpression<Long>>()) } returns updateClause
        every { updateClause.where(any()) } returns updateClause
        every { updateClause.execute() } returns 1L

        repository.decrementLikeCount(postId)
    }

    test("decrementLikeCount should throw PostNotFoundException when post not found or likeCount is already 0") {
        val postId = Tsid.generateLong()

        every { queryFactory.update(post) } returns updateClause
        every { updateClause.set(any<Path<Long>>(), any<NumberExpression<Long>>()) } returns updateClause
        every { updateClause.where(any()) } returns updateClause
        every { updateClause.execute() } returns 0L

        shouldThrow<PostNotFoundException> {
            repository.decrementLikeCount(postId)
        }
    }

    test("getLikeCount should throw PostNotFoundException when post not found") {
        val postId = Tsid.generateLong()

        every { queryFactory.select(post.likeCount) } returns longQuery
        every { longQuery.from(post) } returns longQuery
        every { longQuery.where(any()) } returns longQuery
        every { longQuery.fetchOne() } returns null

        shouldThrow<PostNotFoundException> {
            repository.getLikeCount(postId)
        }
    }
})
