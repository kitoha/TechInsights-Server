package com.techinsights.domain.entity.post

import com.techinsights.domain.enums.ErrorType
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(
    name = "summary_retry_queue",
    indexes = [
        Index(name = "idx_next_retry_at", columnList = "nextRetryAt"),
        Index(name = "idx_retry_count", columnList = "retryCount")
    ]
)
class SummaryRetryQueue(
    @Id
    val postId: Long,

    @Column(length = 1000)
    val reason: String,

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    val errorType: ErrorType,

    val retryCount: Int = 0,

    val nextRetryAt: Instant,

    val createdAt: Instant = Instant.now(),

    val lastRetryAt: Instant? = null,

    val maxRetries: Int = 5
) {
    fun shouldRetry(): Boolean {
        return retryCount < maxRetries && Instant.now().isAfter(nextRetryAt)
    }

    fun incrementRetry(): SummaryRetryQueue {
        return SummaryRetryQueue(
            postId = postId,
            reason = reason,
            errorType = errorType,
            retryCount = retryCount + 1,
            nextRetryAt = Instant.now().plusSeconds(300L * (1 shl retryCount)), // 지수 백오프
            createdAt = createdAt,
            lastRetryAt = Instant.now(),
            maxRetries = maxRetries
        )
    }
}
