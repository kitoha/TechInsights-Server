package com.techinsights.domain.repository.github

import com.techinsights.domain.dto.github.GithubRepositoryDto
import com.techinsights.domain.entity.github.GithubBookmark
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface GithubBookmarkRepository {
    fun saveAndFlush(bookmark: GithubBookmark): GithubBookmark
    fun findByRepoIdAndUserId(repoId: Long, userId: Long): GithubBookmark?
    fun deleteByRepoIdAndUserId(repoId: Long, userId: Long): Long
    fun findBookmarkedRepos(userId: Long, pageable: Pageable): Page<GithubRepositoryDto>
}
