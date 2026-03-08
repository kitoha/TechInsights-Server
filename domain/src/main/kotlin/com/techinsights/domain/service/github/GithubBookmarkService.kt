package com.techinsights.domain.service.github

import com.techinsights.domain.dto.auth.Requester
import com.techinsights.domain.dto.github.GithubRepositoryDto
import com.techinsights.domain.entity.github.GithubBookmark
import com.techinsights.domain.exception.GithubRepositoryNotFoundException
import com.techinsights.domain.exception.UnauthorizedException
import com.techinsights.domain.repository.github.GithubBookmarkRepository
import com.techinsights.domain.repository.github.GithubRepositoryRepository
import com.techinsights.domain.utils.Tsid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GithubBookmarkService(
    private val githubBookmarkRepository: GithubBookmarkRepository,
    private val githubRepositoryRepository: GithubRepositoryRepository,
    private val githubBookmarkSaveHelper: GithubBookmarkSaveHelper,
) {
    @Transactional
    fun toggleBookmark(repoId: Long, requester: Requester): Boolean {
        val userId = when (requester) {
            is Requester.Authenticated -> requester.userId
            is Requester.Anonymous -> throw UnauthorizedException()
        }

        githubRepositoryRepository.findById(repoId)
            ?: throw GithubRepositoryNotFoundException("GithubRepository not found: $repoId")

        return if (githubBookmarkRepository.findByRepoIdAndUserId(repoId, userId) != null) {
            githubBookmarkRepository.deleteByRepoIdAndUserId(repoId, userId)
            false
        } else {
            githubBookmarkSaveHelper.saveIfAbsent(GithubBookmark(Tsid.generateLong(), repoId, userId))
        }
    }

    @Transactional(readOnly = true)
    fun getMyBookmarks(userId: Long, pageable: Pageable): Page<GithubRepositoryDto> =
        githubBookmarkRepository.findBookmarkedRepos(userId, pageable)
}
