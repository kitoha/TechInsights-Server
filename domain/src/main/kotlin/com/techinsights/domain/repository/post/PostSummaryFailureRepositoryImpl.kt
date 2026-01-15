package com.techinsights.domain.repository.post

import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import com.techinsights.domain.dto.failure.ErrorTypeStatDto
import com.techinsights.domain.entity.post.PostSummaryFailure
import com.techinsights.domain.entity.post.QPostSummaryFailure
import com.techinsights.domain.enums.SummaryErrorType
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PostSummaryFailureRepositoryImpl(
    private val jpaRepository: PostSummaryFailureJpaRepository,
    private val queryFactory: JPAQueryFactory
) : PostSummaryFailureRepository {

    private val failure = QPostSummaryFailure.postSummaryFailure

    override fun save(failure: PostSummaryFailure): PostSummaryFailure {
        return jpaRepository.save(failure)
    }

    override fun saveAll(failures: List<PostSummaryFailure>): List<PostSummaryFailure> {
        return jpaRepository.saveAll(failures)
    }

    override fun findLatestByPostId(postId: Long): PostSummaryFailure? {
        return jpaRepository.findTop1ByPostIdOrderByFailedAtDesc(postId)
    }

    override fun existsByPostIdAndErrorType(postId: Long, errorType: SummaryErrorType): Boolean {
        return jpaRepository.existsByPostIdAndErrorType(postId, errorType)
    }

    override fun findPostIdsWithErrorTypes(errorTypes: List<SummaryErrorType>): List<Long> {
        return queryFactory
            .select(failure.postId)
            .from(failure)
            .where(failure.errorType.`in`(errorTypes))
            .distinct()
            .fetch()
    }

    override fun getErrorTypeStatisticsSince(since: LocalDateTime): List<ErrorTypeStatDto> {
        return queryFactory
            .select(
                Projections.constructor(
                    ErrorTypeStatDto::class.java,
                    failure.errorType,
                    failure.count()
                )
            )
            .from(failure)
            .where(failure.failedAt.goe(since))
            .groupBy(failure.errorType)
            .fetch()
    }

    override fun findRecentFailures(limit: Int): List<PostSummaryFailure> {
        return queryFactory
            .selectFrom(failure)
            .orderBy(failure.failedAt.desc())
            .limit(limit.toLong())
            .fetch()
    }

    override fun deleteOldFailures(before: LocalDateTime): Int {
        return jpaRepository.deleteByFailedAtBefore(before)
    }
}
