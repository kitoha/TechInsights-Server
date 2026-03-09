package com.techinsights.domain.repository.github

import com.querydsl.jpa.impl.JPAQueryFactory
import com.techinsights.domain.dto.github.GithubRepositoryDto
import com.techinsights.domain.entity.github.GithubBookmark
import com.techinsights.domain.entity.github.QGithubBookmark
import com.techinsights.domain.entity.github.QGithubRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class GithubBookmarkRepositoryImpl(
    private val githubBookmarkJpaRepository: GithubBookmarkJpaRepository,
    private val queryFactory: JPAQueryFactory,
) : GithubBookmarkRepository {

    override fun saveAndFlush(bookmark: GithubBookmark): GithubBookmark =
        githubBookmarkJpaRepository.saveAndFlush(bookmark)

    override fun findByRepoIdAndUserId(repoId: Long, userId: Long): GithubBookmark? =
        githubBookmarkJpaRepository.findByRepoIdAndUserId(repoId, userId)

    override fun deleteByRepoIdAndUserId(repoId: Long, userId: Long): Long =
        githubBookmarkJpaRepository.deleteByRepoIdAndUserId(repoId, userId)

    override fun findBookmarkedRepos(userId: Long, pageable: Pageable): Page<GithubRepositoryDto> {
        val gb = QGithubBookmark.githubBookmark
        val gr = QGithubRepository.githubRepository

        val results = queryFactory
            .selectFrom(gr)
            .join(gb).on(gb.repoId.eq(gr.id))
            .where(gb.userId.eq(userId))
            .orderBy(gb.createdAt.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()
            .map { GithubRepositoryDto.fromEntity(it) }

        val total = queryFactory
            .select(gr.id.count())
            .from(gr)
            .join(gb).on(gb.repoId.eq(gr.id))
            .where(gb.userId.eq(userId))
            .fetchOne() ?: 0L

        return PageImpl(results, pageable, total)
    }

    override fun countByUserId(userId: Long): Long =
        githubBookmarkJpaRepository.countByUserId(userId)
}
