package com.techinsights.domain.entity.post

import com.techinsights.domain.enums.SummaryErrorType
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "post_summary_failures")
class PostSummaryFailure(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "post_id", nullable = false)
    val postId: Long,

    @Column(name = "error_type", length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    val errorType: SummaryErrorType,

    @Column(name = "error_message", columnDefinition = "TEXT")
    val errorMessage: String? = null,

    @Column(name = "failed_at", nullable = false)
    val failedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "batch_size")
    val batchSize: Int? = null,

    @Column(name = "is_batch_failure", nullable = false)
    val isBatchFailure: Boolean = false
)
