package com.techinsights.domain.entity.github

import com.techinsights.domain.enums.CommunityStatus
import com.techinsights.domain.enums.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(name = "github_repository_community")
@EntityListeners(AuditingEntityListener::class)
class GithubRepositoryCommunity(
    @Id
    val repoId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "community_status", length = 20)
    var communityStatus: CommunityStatus? = null,

    @Column(name = "community_collected_at")
    var communityCollectedAt: LocalDateTime? = null,

    @Column(name = "community_fetched_at")
    var communityFetchedAt: LocalDateTime? = null,

    @Column(name = "community_raw_mention_count", nullable = false)
    var communityRawMentionCount: Int = 0,

    @Column(name = "community_mention_count", nullable = false)
    var communityMentionCount: Int = 0,

    @Column(name = "sentiment_positive", nullable = false)
    var sentimentPositive: Int = 0,

    @Column(name = "sentiment_neutral", nullable = false)
    var sentimentNeutral: Int = 0,

    @Column(name = "sentiment_negative", nullable = false)
    var sentimentNegative: Int = 0,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "community_highlights", columnDefinition = "jsonb")
    var communityHighlights: String? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "community_insights", columnDefinition = "jsonb")
    var communityInsights: String? = null,

    @Column(name = "community_update_count", nullable = false)
    var communityUpdateCount: Int = 0,

    @Enumerated(EnumType.STRING)
    @Column(name = "community_error_type", length = 50)
    var communityErrorType: ErrorType? = null,

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null,

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime? = null,
)
