package com.techinsights.domain.repository.github

import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import com.techinsights.domain.dto.github.GithubRepositoryDto
import com.techinsights.domain.dto.github.GithubRepositoryCursor
import com.techinsights.domain.dto.github.GithubSummaryDto
import com.techinsights.domain.entity.github.QGithubRepository
import com.techinsights.domain.entity.github.QGithubRepositoryCommunity
import com.techinsights.domain.entity.github.QGithubRepositoryReadme
import com.techinsights.domain.enums.CommunityStatus
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

    override fun findRepositoriesByCursor(
        limit: Int,
        sortType: GithubSortType,
        language: String?,
        cursor: GithubRepositoryCursor?,
    ): List<GithubRepositoryDto> {
        val repo = QGithubRepository.githubRepository
        val readme = QGithubRepositoryReadme.githubRepositoryReadme
        val languageCondition = language?.let { repo.primaryLanguage.eq(it) }
        val cursorCondition = buildCursorCondition(repo, sortType, cursor)

        return queryFactory.select(repo, readme.readmeSummary)
            .from(repo)
            .leftJoin(readme).on(readme.repoId.eq(repo.id))
            .where(languageCondition, cursorCondition)
            .orderBy(*buildOrderSpecifiers(repo, sortType))
            .limit(limit.toLong())
            .fetch()
            .map { tuple ->
                GithubRepositoryDto.fromEntity(tuple[repo]!!)
                    .copy(readmeSummary = tuple[readme.readmeSummary])
            }
    }

    override fun findRepositories(
        pageable: Pageable,
        sortType: GithubSortType,
        language: String?,
    ): Page<GithubRepositoryDto> {
        val repo = QGithubRepository.githubRepository
        val readme = QGithubRepositoryReadme.githubRepositoryReadme

        val languageCondition = language?.let { repo.primaryLanguage.eq(it) }

        val results = queryFactory.select(repo, readme.readmeSummary)
            .from(repo)
            .leftJoin(readme).on(readme.repoId.eq(repo.id))
            .where(languageCondition)
            .orderBy(*buildOrderSpecifiers(repo, sortType))
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()
            .map { tuple ->
                GithubRepositoryDto.fromEntity(tuple[repo]!!)
                    .copy(readmeSummary = tuple[readme.readmeSummary])
            }

        val total = queryFactory.select(repo.id.count())
            .from(repo)
            .where(languageCondition)
            .fetchOne() ?: 0L

        return PageImpl(results, pageable, total)
    }

    override fun findById(id: Long): GithubRepositoryDto? {
        val repo = QGithubRepository.githubRepository
        val community = QGithubRepositoryCommunity.githubRepositoryCommunity
        val readme = QGithubRepositoryReadme.githubRepositoryReadme

        return queryFactory.select(repo, community, readme.readmeSummary)
            .from(repo)
            .leftJoin(community).on(community.repoId.eq(repo.id))
            .leftJoin(readme).on(readme.repoId.eq(repo.id))
            .where(repo.id.eq(id))
            .fetchOne()
            ?.let { tuple ->
                GithubRepositoryDto.fromEntity(tuple[repo]!!).copy(
                    readmeSummary = tuple[readme.readmeSummary],
                    communityStatus = tuple[community]?.communityStatus,
                    communitySentiment = tuple[community]?.communitySentiment,
                    communityInsights = tuple[community]?.communityInsights,
                    communityCollectedAt = tuple[community]?.communityCollectedAt,
                    communityMentionCount = tuple[community]?.communityMentionCount,
                    communityHighlights = tuple[community]?.communityHighlights,
                )
            }
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

        return queryFactory.select(repo, readme.readmeSummary)
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
                GithubRepositoryDto.fromEntity(tuple[repo]!!)
                    .copy(readmeSummary = tuple[readme.readmeSummary])
            }
    }

    override fun findForCommunityCollect(
        pageSize: Int,
        afterCollectedAt: LocalDateTime?,
        afterId: Long?,
        noMentionsRefreshAfter: LocalDateTime,
        normalRefreshAfter: LocalDateTime,
    ): List<GithubRepositoryDto> {
        val repo = QGithubRepository.githubRepository
        val community = QGithubRepositoryCommunity.githubRepositoryCommunity

        val neverCollected = community.repoId.isNull
        val noMentionsRefresh = community.communityStatus.eq(CommunityStatus.NO_MENTIONS)
            .and(community.communityCollectedAt.lt(noMentionsRefreshAfter))
        val normalRefresh = community.communityStatus.ne(CommunityStatus.NO_MENTIONS)
            .and(community.communityCollectedAt.lt(normalRefreshAfter))

        val cursorCondition = if (afterCollectedAt != null && afterId != null) {
            community.communityCollectedAt.gt(afterCollectedAt)
                .or(community.communityCollectedAt.eq(afterCollectedAt).and(repo.id.gt(afterId)))
        } else if (afterCollectedAt == null && afterId != null) {
            community.communityCollectedAt.isNotNull
                .or(community.communityCollectedAt.isNull.and(repo.id.gt(afterId)))
        } else null

        return queryFactory.select(repo, community)
            .from(repo)
            .leftJoin(community).on(community.repoId.eq(repo.id))
            .where(
                neverCollected.or(noMentionsRefresh).or(normalRefresh),
                cursorCondition,
            )
            .orderBy(community.communityCollectedAt.asc().nullsFirst(), repo.id.asc())
            .limit(pageSize.toLong())
            .fetch()
            .map { tuple ->
                GithubRepositoryDto.fromEntity(tuple[repo]!!).copy(
                    communityStatus = tuple[community]?.communityStatus,
                    communityCollectedAt = tuple[community]?.communityCollectedAt,
                    communityMentionCount = tuple[community]?.communityMentionCount,
                    communityRawMentionCount = tuple[community]?.communityRawMentionCount,
                    communityUpdateCount = tuple[community]?.communityUpdateCount ?: 0,
                    communityHighlights = tuple[community]?.communityHighlights,
                )
            }
    }

    override fun findForCommunityAnalyze(
        pageSize: Int,
        afterId: Long?,
    ): List<GithubRepositoryDto> {
        val repo = QGithubRepository.githubRepository
        val community = QGithubRepositoryCommunity.githubRepositoryCommunity

        val cursorCondition = afterId?.let { repo.id.gt(it) }

        return queryFactory.select(repo, community)
            .from(repo)
            .join(community).on(community.repoId.eq(repo.id))
            .where(
                community.communityStatus.eq(CommunityStatus.PENDING),
                cursorCondition,
            )
            .orderBy(repo.id.asc())
            .limit(pageSize.toLong())
            .fetch()
            .map { tuple ->
                GithubRepositoryDto.fromEntity(tuple[repo]!!).copy(
                    communityStatus = tuple[community]?.communityStatus,
                    communityCollectedAt = tuple[community]?.communityCollectedAt,
                    communityMentionCount = tuple[community]?.communityMentionCount,
                    communityRawMentionCount = tuple[community]?.communityRawMentionCount,
                    communityUpdateCount = tuple[community]?.communityUpdateCount ?: 0,
                    communityHighlights = tuple[community]?.communityHighlights,
                )
            }
    }

    override fun findSimilarRepositories(
        targetVector: String,
        limit: Long,
    ): List<GithubRepositoryWithDistance> =
        githubRepositoryJpaRepository.findSimilarRepositories(targetVector, limit)

    override fun countAndSumStars(language: String?): GithubSummaryDto {
        val repo = QGithubRepository.githubRepository
        val languageCondition = language?.let { repo.primaryLanguage.eq(it) }
        
        val tuple = queryFactory.select(repo.count(), repo.starCount.sum())
            .from(repo)
            .where(languageCondition)
            .fetchOne()
            
        return GithubSummaryDto(
            totalRepositories = tuple?.get(repo.count()) ?: 0L,
            totalStars = tuple?.get(repo.starCount.sum()) ?: 0L
        )
    }

    private fun buildOrderSpecifiers(
        repo: QGithubRepository,
        sortType: GithubSortType,
    ): Array<OrderSpecifier<*>> = when (sortType) {
        GithubSortType.STARS          -> arrayOf(repo.starCount.desc(), repo.id.desc())
        GithubSortType.LATEST         -> arrayOf(repo.pushedAt.desc(), repo.id.desc())
        GithubSortType.TRENDING       -> arrayOf(repo.weeklyStarDelta.desc(), repo.starCount.desc(), repo.id.desc())
        GithubSortType.DAILY_TRENDING -> arrayOf(repo.dailyStarDelta.desc(), repo.starCount.desc(), repo.id.desc())
    }

    private fun buildCursorCondition(
        repo: QGithubRepository,
        sortType: GithubSortType,
        cursor: GithubRepositoryCursor?,
    ): BooleanExpression? {
        if (cursor == null) return null

        return when (sortType) {
            GithubSortType.STARS -> repo.starCount.lt(cursor.primaryAsLong())
                .or(repo.starCount.eq(cursor.primaryAsLong()).and(repo.id.lt(cursor.id)))

            GithubSortType.LATEST -> repo.pushedAt.lt(cursor.primaryAsDateTime())
                .or(repo.pushedAt.eq(cursor.primaryAsDateTime()).and(repo.id.lt(cursor.id)))

            GithubSortType.TRENDING -> repo.weeklyStarDelta.lt(cursor.primaryAsLong())
                .or(
                    repo.weeklyStarDelta.eq(cursor.primaryAsLong()).and(
                        repo.starCount.lt(cursor.secondaryAsLong())
                            .or(repo.starCount.eq(cursor.secondaryAsLong()).and(repo.id.lt(cursor.id)))
                    )
                )

            GithubSortType.DAILY_TRENDING -> repo.dailyStarDelta.lt(cursor.primaryAsLong())
                .or(
                    repo.dailyStarDelta.eq(cursor.primaryAsLong()).and(
                        repo.starCount.lt(cursor.secondaryAsLong())
                            .or(repo.starCount.eq(cursor.secondaryAsLong()).and(repo.id.lt(cursor.id)))
                    )
                )
        }
    }
}
