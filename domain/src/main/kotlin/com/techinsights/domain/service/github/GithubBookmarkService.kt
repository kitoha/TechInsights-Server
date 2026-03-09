package com.techinsights.domain.service.github

import com.techinsights.domain.dto.auth.Requester
import com.techinsights.domain.dto.github.GithubRepositoryDto
import com.techinsights.domain.entity.github.GithubBookmark
import com.techinsights.domain.exception.GithubRepositoryNotFoundException
import com.techinsights.domain.exception.UnauthorizedException
import com.techinsights.domain.repository.github.GithubBookmarkRepository
import com.techinsights.domain.repository.github.GithubRepositoryRepository
import com.techinsights.domain.utils.Tsid
import com.techinsights.domain.utils.decode
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
    fun toggleBookmark(repoId: String, requester: Requester): Boolean {
        val userId = when (requester) {
            is Requester.Authenticated -> requester.userId
            is Requester.Anonymous -> throw UnauthorizedException()
        }

        val repoIdLong = repoId.decode()

        githubRepositoryRepository.findById(repoIdLong)
            ?: throw GithubRepositoryNotFoundException("GithubRepository not found: $repoId")

        return if (githubBookmarkRepository.findByRepoIdAndUserId(repoIdLong, userId) != null) {
            githubBookmarkRepository.deleteByRepoIdAndUserId(repoIdLong, userId)
            false
        } else {
            githubBookmarkSaveHelper.saveIfAbsent(GithubBookmark(Tsid.generateLong(), repoIdLong, userId))
        }
    }

    @Transactional(readOnly = true)
    fun getMyBookmarks(userId: Long, pageable: Pageable): Page<GithubRepositoryDto> =
        githubBookmarkRepository.findBookmarkedRepos(userId, pageable)

    @Transactional(readOnly = true)
    fun countMyBookmarks(userId: Long): Long =
        githubBookmarkRepository.countByUserId(userId)
}
