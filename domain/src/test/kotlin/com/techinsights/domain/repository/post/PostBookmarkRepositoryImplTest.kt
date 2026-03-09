package com.techinsights.domain.repository.post

import com.querydsl.jpa.impl.JPAQuery
import com.querydsl.jpa.impl.JPAQueryFactory
import com.techinsights.domain.entity.company.QCompany
import com.techinsights.domain.entity.post.Post
import com.techinsights.domain.entity.post.PostBookmark
import com.techinsights.domain.entity.post.QPost
import com.techinsights.domain.entity.post.QPostBookmark
import com.techinsights.domain.utils.Tsid
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.PageRequest

class PostBookmarkRepositoryImplTest : FunSpec({

    val jpaRepository = mockk<PostBookmarkJpaRepository>()
    val queryFactory = mockk<JPAQueryFactory>()
    val repository = PostBookmarkRepositoryImpl(jpaRepository, queryFactory)

    val postId = Tsid.generateLong()
    val userId = 1L

    beforeTest { clearAllMocks() }

    test("saveAndFlush - jpaRepository.saveAndFlush에 위임하고 결과 반환") {
        // given
        val bookmark = PostBookmark(id = Tsid.generateLong(), postId = postId, userId = userId)
        every { jpaRepository.saveAndFlush(bookmark) } returns bookmark

        // when
        val result = repository.saveAndFlush(bookmark)

        // then
        result shouldBe bookmark
        verify(exactly = 1) { jpaRepository.saveAndFlush(bookmark) }
    }

    test("findByPostIdAndUserId - jpaRepository에 위임하고 결과 반환") {
        // given
        val bookmark = mockk<PostBookmark>()
        every { jpaRepository.findByPostIdAndUserId(postId, userId) } returns bookmark

        // when
        val result = repository.findByPostIdAndUserId(postId, userId)

        // then
        result shouldBe bookmark
        verify(exactly = 1) { jpaRepository.findByPostIdAndUserId(postId, userId) }
    }

    test("findByPostIdAndUserId - 결과 없을 때 null 반환") {
        // given
        every { jpaRepository.findByPostIdAndUserId(postId, userId) } returns null

        // when
        val result = repository.findByPostIdAndUserId(postId, userId)

        // then
        result shouldBe null
    }

    test("deleteByPostIdAndUserId - jpaRepository에 위임하고 삭제 건수 반환") {
        // given
        every { jpaRepository.deleteByPostIdAndUserId(postId, userId) } returns 1L

        // when
        val result = repository.deleteByPostIdAndUserId(postId, userId)

        // then
        result shouldBe 1L
        verify(exactly = 1) { jpaRepository.deleteByPostIdAndUserId(postId, userId) }
    }

    test("findBookmarkedPosts - QueryDSL로 페이지 조회 후 Page<PostDto> 반환") {
        // given
        val pageable = PageRequest.of(0, 20)
        val query = mockk<JPAQuery<Post>>()
        val countQuery = mockk<JPAQuery<Long>>()

        every { queryFactory.selectFrom(QPost.post) } returns query
        every { query.leftJoin(QPost.post.company, QCompany.company) } returns query
        every { query.fetchJoin() } returns query
        every { query.join(QPostBookmark.postBookmark) } returns query
        every { query.on(any()) } returns query
        every { query.where(any()) } returns query
        every { query.orderBy(any()) } returns query
        every { query.offset(0L) } returns query
        every { query.limit(20L) } returns query
        every { query.fetch() } returns emptyList()

        every { queryFactory.select(QPost.post.id.count()) } returns countQuery
        every { countQuery.from(QPost.post) } returns countQuery
        every { countQuery.join(QPostBookmark.postBookmark) } returns countQuery
        every { countQuery.on(any()) } returns countQuery
        every { countQuery.where(any()) } returns countQuery
        every { countQuery.fetchOne() } returns 0L

        // when
        val result = repository.findBookmarkedPosts(userId, pageable)

        // then
        result.totalElements shouldBe 0
        result.content.size shouldBe 0
        verify(exactly = 1) { query.fetch() }
        verify(exactly = 1) { countQuery.fetchOne() }
    }
})
