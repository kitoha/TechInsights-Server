package com.techinsights.domain.repository.post

import com.techinsights.domain.entity.post.PostSummaryFailure
import com.techinsights.domain.enums.SummaryErrorType
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface PostSummaryFailureJpaRepository : JpaRepository<PostSummaryFailure, Long> {

    fun findTop1ByPostIdOrderByFailedAtDesc(postId: Long): PostSummaryFailure?

    fun existsByPostIdAndErrorType(postId: Long, errorType: SummaryErrorType): Boolean

    fun deleteByFailedAtBefore(before: LocalDateTime): Int
}
