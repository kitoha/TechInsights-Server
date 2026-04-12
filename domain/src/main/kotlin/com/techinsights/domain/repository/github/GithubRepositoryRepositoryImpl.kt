package com.techinsights.domain.repository.github

import com.querydsl.core.types.OrderSpecifier
import com.querydsl.jpa.impl.JPAQueryFactory
import com.techinsights.domain.dto.github.GithubRepositoryDto
import com.techinsights.domain.entity.github.QGithubRepository
import com.techinsights.domain.entity.github.QGithubRepositoryReadme
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
            GithubSortType.STARS          -> arrayOf(repo.starCount.desc(), repo.id.desc())
            GithubSortType.LATEST         -> arrayOf(repo.pushedAt.desc(), repo.id.desc())
            GithubSortType.TRENDING       -> arrayOf(repo.weeklyStarDelta.desc(), repo.starCount.desc(), repo.id.desc())
            GithubSortType.DAILY_TRENDING -> arrayOf(repo.dailyStarDelta.desc(), repo.starCount.desc(), repo.id.desc())
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
        val readme = QGithubRepositoryReadme.githubRepositoryReadme

        val cursorCondition = if (afterStarCount != null && afterId != null) {
            repo.starCount.lt(afterStarCount)
                .or(repo.starCount.eq(afterStarCount).and(repo.id.lt(afterId)))
        } else null

        // LEFT JOIN — readme 행이 없으면 한번도 시도 안 한 레포
        val neverAttempted = readme.repoId.isNull

        val retryCondition = if (retryAfter != null && retryableErrorTypes.isNotEmpty()) {
            readme.readmeSummaryErrorType.`in`(retryableErrorTypes.map { it.name })
                .and(readme.readmeSummarizedAt.lt(retryAfter))
        } else null

        val mainCondition = retryCondition?.let { neverAttempted.or(it) } ?: neverAttempted

        return queryFactory.selectFrom(repo)
            .leftJoin(readme).on(readme.repoId.eq(repo.id))
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
        val readme = QGithubRepositoryReadme.githubRepositoryReadme

        val cursorCondition = if (afterStarCount != null && afterId != null) {
            repo.starCount.lt(afterStarCount)
                .or(repo.starCount.eq(afterStarCount).and(repo.id.lt(afterId)))
        } else null

        return queryFactory.select(repo, readme)
            .from(repo)
            .join(readme).on(readme.repoId.eq(repo.id))
            .where(
                readme.readmeSummarizedAt.isNotNull,
                readme.readmeSummary.isNotNull,
                readme.readmeEmbeddedAt.isNull,
                cursorCondition,
            )
            .orderBy(repo.starCount.desc(), repo.id.desc())
            .limit(pageSize.toLong())
            .fetch()
            .map { tuple ->
                GithubRepositoryDto.fromEntity(tuple.get(repo)!!)
                    .copy(readmeSummary = tuple.get(readme)?.readmeSummary)
            }
    }

    // Step 6에서 GithubRepositoryCommunity 기반으로 재구현 예정
    override fun findForCommunityCollect(
        pageSize: Int,
        afterCollectedAt: LocalDateTime?,
        afterId: Long?,
        noMentionsRefreshAfter: LocalDateTime,
        normalRefreshAfter: LocalDateTime,
    ): List<GithubRepositoryDto> = emptyList()

    // Step 7에서 GithubRepositoryCommunity 기반으로 재구현 예정
    override fun findForCommunityAnalyze(
        pageSize: Int,
        afterId: Long?,
    ): List<GithubRepositoryDto> = emptyList()

    override fun findSimilarRepositories(
        targetVector: String,
        limit: Long,
    ): List<GithubRepositoryWithDistance> =
        githubRepositoryJpaRepository.findSimilarRepositories(targetVector, limit)
}
