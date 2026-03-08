package com.techinsights.domain.repository.github

import com.querydsl.jpa.impl.JPAQuery
import com.querydsl.jpa.impl.JPAQueryFactory
import com.techinsights.domain.entity.github.GithubBookmark
import com.techinsights.domain.entity.github.QGithubBookmark
import com.techinsights.domain.entity.github.QGithubRepository
import com.techinsights.domain.utils.Tsid
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.PageRequest

class GithubBookmarkRepositoryImplTest : FunSpec({

    val jpaRepository = mockk<GithubBookmarkJpaRepository>()
    val queryFactory = mockk<JPAQueryFactory>()
    val repository = GithubBookmarkRepositoryImpl(jpaRepository, queryFactory)

    val repoId = 100L
    val userId = 1L

    beforeTest { clearAllMocks() }

    test("saveAndFlush - jpaRepository.saveAndFlush에 위임하고 결과 반환") {
        // given
        val bookmark = GithubBookmark(id = Tsid.generateLong(), repoId = repoId, userId = userId)
        every { jpaRepository.saveAndFlush(bookmark) } returns bookmark

        // when
        val result = repository.saveAndFlush(bookmark)

        // then
        result shouldBe bookmark
        verify(exactly = 1) { jpaRepository.saveAndFlush(bookmark) }
    }

    test("findByRepoIdAndUserId - jpaRepository에 위임하고 결과 반환") {
        // given
        val bookmark = mockk<GithubBookmark>()
        every { jpaRepository.findByRepoIdAndUserId(repoId, userId) } returns bookmark

        // when
        val result = repository.findByRepoIdAndUserId(repoId, userId)

        // then
        result shouldBe bookmark
        verify(exactly = 1) { jpaRepository.findByRepoIdAndUserId(repoId, userId) }
    }

    test("findByRepoIdAndUserId - 결과 없을 때 null 반환") {
        // given
        every { jpaRepository.findByRepoIdAndUserId(repoId, userId) } returns null

        // when
        val result = repository.findByRepoIdAndUserId(repoId, userId)

        // then
        result shouldBe null
    }

    test("deleteByRepoIdAndUserId - jpaRepository에 위임하고 삭제 건수 반환") {
        // given
        every { jpaRepository.deleteByRepoIdAndUserId(repoId, userId) } returns 1L

        // when
        val result = repository.deleteByRepoIdAndUserId(repoId, userId)

        // then
        result shouldBe 1L
        verify(exactly = 1) { jpaRepository.deleteByRepoIdAndUserId(repoId, userId) }
    }

    test("findBookmarkedRepos - QueryDSL로 페이지 조회 후 Page<GithubRepositoryDto> 반환") {
        // given
        val pageable = PageRequest.of(0, 20)
        val query = mockk<JPAQuery<com.techinsights.domain.entity.github.GithubRepository>>()
        val countQuery = mockk<JPAQuery<Long>>()

        every { queryFactory.selectFrom(QGithubRepository.githubRepository) } returns query
        every { query.join(QGithubBookmark.githubBookmark) } returns query
        every { query.on(any()) } returns query
        every { query.where(any()) } returns query
        every { query.orderBy(any()) } returns query
        every { query.offset(0L) } returns query
        every { query.limit(20L) } returns query
        every { query.fetch() } returns emptyList()

        every { queryFactory.select(QGithubRepository.githubRepository.id.count()) } returns countQuery
        every { countQuery.from(QGithubRepository.githubRepository) } returns countQuery
        every { countQuery.join(QGithubBookmark.githubBookmark) } returns countQuery
        every { countQuery.on(any()) } returns countQuery
        every { countQuery.where(any()) } returns countQuery
        every { countQuery.fetchOne() } returns 0L

        // when
        val result = repository.findBookmarkedRepos(userId, pageable)

        // then
        result.totalElements shouldBe 0
        result.content.size shouldBe 0
        verify(exactly = 1) { query.fetch() }
        verify(exactly = 1) { countQuery.fetchOne() }
    }
})
