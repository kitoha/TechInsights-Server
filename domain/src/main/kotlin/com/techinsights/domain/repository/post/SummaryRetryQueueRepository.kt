package com.techinsights.domain.repository.post

import com.techinsights.domain.entity.post.SummaryRetryQueue
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface SummaryRetryQueueRepository : JpaRepository<SummaryRetryQueue, Long> {

    @Query("""
        SELECT s FROM SummaryRetryQueue s
        WHERE s.nextRetryAt <= :time
        AND s.retryCount < :maxRetries
        ORDER BY s.nextRetryAt ASC
    """)
    fun findRetryableItems(
        @Param("time") time: Instant,
        @Param("maxRetries") maxRetries: Int
    ): List<SummaryRetryQueue>

    @Query("""
        SELECT s FROM SummaryRetryQueue s
        WHERE s.retryCount >= s.maxRetries
        ORDER BY s.createdAt DESC
    """)
    fun findExhaustedItems(): List<SummaryRetryQueue>
}
