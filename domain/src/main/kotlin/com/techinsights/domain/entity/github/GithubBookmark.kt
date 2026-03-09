package com.techinsights.domain.entity.github

import com.techinsights.domain.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "github_bookmarks")
class GithubBookmark(
    @Id val id: Long,
    @Column(name = "repo_id", nullable = false) val repoId: Long,
    @Column(name = "user_id", nullable = false) val userId: Long,
) : BaseEntity()
