package com.techinsights.domain.entity.community

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "community_posts")
class CommunityPost(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "repo_id", nullable = false)
    val repoId: Long,

    @Column(name = "platform", length = 20, nullable = false)
    val platform: String,

    @Column(name = "url", nullable = false, columnDefinition = "TEXT")
    val url: String,

    @Column(name = "title", nullable = false, columnDefinition = "TEXT")
    val title: String,

    @Column(name = "score", nullable = false)
    var score: Int = 0,

    @Column(name = "comment_count", nullable = false)
    var commentCount: Int = 0,

    @Column(name = "post_created_at")
    val postCreatedAt: LocalDateTime? = null,

    @Column(name = "sentiment", length = 20)
    var sentiment: String? = null,

    @Column(name = "sentiment_analyzed_at")
    var sentimentAnalyzedAt: LocalDateTime? = null,

    @Column(name = "collected_at", nullable = false)
    val collectedAt: LocalDateTime = LocalDateTime.now(),
)
