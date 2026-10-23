package com.techinsights.domain.repository.post

import com.querydsl.core.types.Path
import com.querydsl.core.types.dsl.NumberExpression
import com.querydsl.jpa.impl.JPAQuery
import com.querydsl.jpa.impl.JPAQueryFactory
import com.querydsl.jpa.impl.JPAUpdateClause
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.entity.company.Company
import com.techinsights.domain.entity.post.Post
import com.techinsights.domain.entity.post.QPost
import com.techinsights.domain.enums.Category
import com.techinsights.domain.exception.PostNotFoundException
import com.techinsights.domain.utils.Tsid
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import jakarta.persistence.EntityManager
import java.time.LocalDateTime

class PostRepositoryTest : FunSpec({

    val postJpaRepository = mockk<PostJpaRepository>()
    val queryFactory = mockk<JPAQueryFactory>()
    val repository = PostRepositoryImpl(postJpaRepository, queryFactory, mockk<EntityManager>())

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

    test("updateSummaryResults should write only summary-owned columns") {
        val companyEntity = Company(id = 1L, name = "c", blogUrl = "u", logoImageName = "l")
        val entity = Post(
            id = 10L, title = "t", preview = null, url = "https://a.com/1", content = "raw",
            publishedAt = LocalDateTime.of(2024, 1, 1, 0, 0), company = companyEntity,
            viewCount = 7L, likeCount = 3L, summaryFailureCount = 2
        )
        val dto = PostDto.fromEntity(entity).copy(
            content = "summary", preview = "preview", isSummary = true, categories = setOf(Category.AI)
        )
        val updatedPaths = mutableListOf<Path<*>>()

        every { queryFactory.update(post) } returns updateClause
        every { updateClause.set(any<Path<Any>>(), any<Any>()) } answers {
            updatedPaths.add(firstArg())
            updateClause
        }
        every { updateClause.where(any()) } returns updateClause
        every { updateClause.execute() } returns 1L
        every { postJpaRepository.findAllById(any<Iterable<Long>>()) } returns listOf(entity)

        repository.updateSummaryResults(listOf(dto))

        updatedPaths.map { it.toString() } shouldContainExactlyInAnyOrder
            listOf(post.content, post.preview, post.isSummary, post.updatedAt).map { it.toString() }
        entity.viewCount shouldBe 7L
        entity.likeCount shouldBe 3L
        entity.summaryFailureCount shouldBe 2
        entity.categoryValues shouldBe setOf(Category.AI)
    }

    test("updateSummaryResults should throw PostNotFoundException when no row is updated") {
        val companyEntity = Company(id = 1L, name = "c", blogUrl = "u", logoImageName = "l")
        val dto = PostDto.fromEntity(
            Post(
                id = 10L, title = "t", preview = null, url = "https://a.com/1", content = "raw",
                publishedAt = LocalDateTime.of(2024, 1, 1, 0, 0), company = companyEntity
            )
        )

        every { queryFactory.update(post) } returns updateClause
        every { updateClause.set(any<Path<Any>>(), any<Any>()) } returns updateClause
        every { updateClause.where(any()) } returns updateClause
        every { updateClause.execute() } returns 0L

        shouldThrow<PostNotFoundException> { repository.updateSummaryResults(listOf(dto)) }
    }
})
