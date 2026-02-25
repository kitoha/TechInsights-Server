package com.techinsights.domain.repository.github

import com.querydsl.core.types.OrderSpecifier
import com.querydsl.jpa.impl.JPAQueryFactory
import com.techinsights.domain.dto.github.GithubRepositoryDto
import com.techinsights.domain.entity.github.QGithubRepository
import com.techinsights.domain.enums.GithubSortType
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class GithubRepositoryRepositoryImpl(
    private val githubRepositoryJpaRepository: GithubRepositoryJpaRepository,
    private val queryFactory: JPAQueryFactory,
) : GithubRepositoryRepository {

    override fun findRepositories(
        pageable: Pageable,
        sortType: GithubSortType,
        language: String?,
    ): Page<GithubRepositoryDto> {
        val repo = QGithubRepository.githubRepository

        val orderSpecifier: OrderSpecifier<*> = when (sortType) {
            GithubSortType.STARS -> repo.starCount.desc()
            GithubSortType.LATEST -> repo.pushedAt.desc()
            GithubSortType.TRENDING -> repo.weeklyStarDelta.desc()
        }

        val languageCondition = language?.let { repo.primaryLanguage.eq(it) }

        val results = queryFactory.selectFrom(repo)
            .where(languageCondition)
            .orderBy(orderSpecifier)
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()
            .map { GithubRepositoryDto.fromEntity(it) }

        val total = queryFactory.select(repo.id.count())
            .from(repo)
            .where(languageCondition)
            .fetchOne() ?: 0L

        return PageImpl(results, pageable, total)
    }

    override fun findById(id: Long): GithubRepositoryDto? {
        val repo = QGithubRepository.githubRepository

        return queryFactory.selectFrom(repo)
            .where(repo.id.eq(id))
            .fetchOne()
            ?.let { GithubRepositoryDto.fromEntity(it) }
    }
}
