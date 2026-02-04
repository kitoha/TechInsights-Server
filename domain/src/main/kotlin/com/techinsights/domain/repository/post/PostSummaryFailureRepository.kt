package com.techinsights.domain.repository.post

import com.techinsights.domain.dto.failure.ErrorTypeStatDto
import com.techinsights.domain.entity.post.PostSummaryFailure
import com.techinsights.domain.enums.SummaryErrorType
import java.time.LocalDateTime

interface PostSummaryFailureRepository {

    fun save(failure: PostSummaryFailure): PostSummaryFailure

    fun saveAll(failures: List<PostSummaryFailure>): List<PostSummaryFailure>

    fun findLatestByPostId(postId: Long): PostSummaryFailure?

    fun existsByPostIdAndErrorType(postId: Long, errorType: SummaryErrorType): Boolean

    fun findPostIdsWithErrorTypes(errorTypes: List<SummaryErrorType>): List<Long>

    fun getErrorTypeStatisticsSince(since: LocalDateTime): List<ErrorTypeStatDto>

    fun findRecentFailures(limit: Int): List<PostSummaryFailure>

    fun deleteOldFailures(before: LocalDateTime): Int
}
