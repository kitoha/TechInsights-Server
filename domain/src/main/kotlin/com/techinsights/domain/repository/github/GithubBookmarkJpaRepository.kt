package com.techinsights.domain.repository.github

import com.techinsights.domain.entity.github.GithubBookmark
import org.springframework.data.jpa.repository.JpaRepository

interface GithubBookmarkJpaRepository : JpaRepository<GithubBookmark, Long> {

    fun findByRepoIdAndUserId(repoId: Long, userId: Long): GithubBookmark?

    fun deleteByRepoIdAndUserId(repoId: Long, userId: Long): Long

    fun countByUserId(userId: Long): Long
}
