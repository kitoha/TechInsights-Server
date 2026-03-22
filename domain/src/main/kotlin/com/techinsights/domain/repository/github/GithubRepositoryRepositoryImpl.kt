package com.techinsights.domain.repository.github

import com.querydsl.core.types.OrderSpecifier
import com.querydsl.jpa.impl.JPAQueryFactory
import com.techinsights.domain.dto.github.GithubRepositoryDto
import com.techinsights.domain.entity.github.QGithubRepository
import com.techinsights.domain.enums.ErrorType
import com.techinsights.domain.enums.GithubSortType
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

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

        val orderSpecifiers: Array<OrderSpecifier<*>> = when (sortType) {
            GithubSortType.STARS    -> arrayOf(repo.starCount.desc(), repo.id.desc())
            GithubSortType.LATEST   -> arrayOf(repo.pushedAt.desc(), repo.id.desc())
            GithubSortType.TRENDING -> arrayOf(repo.weeklyStarDelta.desc(), repo.starCount.desc(), repo.id.desc())
        }

        val languageCondition = language?.let { repo.primaryLanguage.eq(it) }

        val results = queryFactory.selectFrom(repo)
            .where(languageCondition)
            .orderBy(*orderSpecifiers)
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

    override fun findUnsummarized(
        pageSize: Int,
        afterStarCount: Long?,
        afterId: Long?,
        retryAfter: LocalDateTime?,
        retryableErrorTypes: Set<ErrorType>,
    ): List<GithubRepositoryDto> {
        val repo = QGithubRepository.githubRepository

        val cursorCondition = if (afterStarCount != null && afterId != null) {
            repo.starCount.lt(afterStarCount)
                .or(repo.starCount.eq(afterStarCount).and(repo.id.lt(afterId)))
        } else null

        val neverAttempted = repo.readmeSummarizedAt.isNull

        val retryCondition = if (retryAfter != null && retryableErrorTypes.isNotEmpty()) {
            repo.readmeSummaryErrorType.stringValue().`in`(retryableErrorTypes.map { it.name })
                .and(repo.readmeSummarizedAt.lt(retryAfter))
        } else null

        val mainCondition = retryCondition?.let { neverAttempted.or(it) } ?: neverAttempted

        return queryFactory.selectFrom(repo)
            .where(mainCondition, cursorCondition)
            .orderBy(repo.starCount.desc(), repo.id.desc())
            .limit(pageSize.toLong())
            .fetch()
            .map { GithubRepositoryDto.fromEntity(it) }
    }

    override fun findUnembedded(
        pageSize: Int,
        afterStarCount: Long?,
        afterId: Long?,
    ): List<GithubRepositoryDto> {
        val repo = QGithubRepository.githubRepository

        val cursorCondition = if (afterStarCount != null && afterId != null) {
            repo.starCount.lt(afterStarCount)
                .or(repo.starCount.eq(afterStarCount).and(repo.id.lt(afterId)))
        } else null

        return queryFactory.selectFrom(repo)
            .where(
                repo.readmeSummarizedAt.isNotNull,   // 요약이 완료된 것만
                repo.readmeSummary.isNotNull,         // 실제 summary가 있는 것만 (실패 마킹 제외)
                repo.readmeEmbeddedAt.isNull,         // 아직 임베딩 안 된 것
                cursorCondition,
            )
            .orderBy(repo.starCount.desc(), repo.id.desc())
            .limit(pageSize.toLong())
            .fetch()
            .map { GithubRepositoryDto.fromEntity(it) }
    }

    override fun findSimilarRepositories(
        targetVector: String,
        limit: Long,
    ): List<GithubRepositoryWithDistance> =
        githubRepositoryJpaRepository.findSimilarRepositories(targetVector, limit)
}
