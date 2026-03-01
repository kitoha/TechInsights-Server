package com.techinsights.domain.entity.github

import com.techinsights.domain.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "github_repositories")
class GithubRepository(
    @Id
    val id: Long,

    @Column(name = "repo_name", length = 255, nullable = false)
    val repoName: String,

    @Column(name = "full_name", length = 500, nullable = false, unique = true)
    val fullName: String,

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String?,

    @Column(name = "html_url", length = 1000, nullable = false)
    val htmlUrl: String,

    @Column(name = "star_count", nullable = false)
    var starCount: Long = 0L,

    @Column(name = "fork_count", nullable = false)
    var forkCount: Long = 0L,

    @Column(name = "primary_language", length = 100)
    var primaryLanguage: String?,

    @Column(name = "owner_name", length = 255, nullable = false)
    val ownerName: String,

    @Column(name = "owner_avatar_url", length = 1000)
    var ownerAvatarUrl: String?,

    @Column(name = "topics", columnDefinition = "TEXT")
    var topics: String?,

    @Column(name = "pushed_at", nullable = false)
    var pushedAt: LocalDateTime,

    @Column(name = "fetched_at", nullable = false)
    var fetchedAt: LocalDateTime,

    @Column(name = "weekly_star_delta", nullable = false)
    var weeklyStarDelta: Long = 0L,

    @Column(name = "star_count_prev_week")
    var starCountPrevWeek: Long? = null,

    @Column(name = "star_count_prev_week_updated_at")
    var starCountPrevWeekUpdatedAt: LocalDateTime? = null,

    @Column(name = "readme_summary", columnDefinition = "TEXT")
    var readmeSummary: String? = null,

    @Column(name = "readme_summarized_at")
    var readmeSummarizedAt: LocalDateTime? = null,
) : BaseEntity()
