package com.techinsights.domain.service.github

import com.techinsights.domain.config.cache.CacheConfig
import com.techinsights.domain.dto.github.GithubRepositoryDto
import com.techinsights.domain.dto.github.GithubRepositoryCursor
import com.techinsights.domain.dto.github.GithubRepositoryCursorPage
import com.techinsights.domain.dto.github.GithubSummaryDto
import com.techinsights.domain.enums.GithubSortType
import com.techinsights.domain.exception.GithubRepositoryNotFoundException
import com.techinsights.domain.repository.github.GithubRepositoryRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GithubTrendingService(
    private val githubRepositoryRepository: GithubRepositoryRepository,
) {

    @Cacheable(cacheNames = [CacheConfig.GITHUB_TRENDING])
    @Transactional(readOnly = true)
    fun getSummary(sort: GithubSortType, language: String?): GithubSummaryDto {
        return githubRepositoryRepository.countAndSumStars(sort, language)
    }

    @Cacheable(cacheNames = [CacheConfig.GITHUB_TRENDING])
    @Transactional(readOnly = true)
    fun getRepositoriesByCursor(

        cursor: String?,
        size: Int,
        sort: GithubSortType,
        language: String?,
    ): GithubRepositoryCursorPage {
        val decodedCursor = cursor?.let { GithubRepositoryCursor.decode(it, sort) }
        val results = githubRepositoryRepository.findRepositoriesByCursor(size + 1, sort, language, decodedCursor)
        val hasNext = results.size > size
        val content = if (hasNext) results.take(size) else results
        val nextCursor = if (hasNext && content.isNotEmpty()) {
            GithubRepositoryCursor.fromDto(content.last(), sort).encode()
        } else {
            null
        }

        return GithubRepositoryCursorPage(
            content = content,
            size = size,
            hasNext = hasNext,
            nextCursor = nextCursor,
        )
    }

    @Cacheable(cacheNames = [CacheConfig.GITHUB_TRENDING])
    @Transactional(readOnly = true)
    fun getRepositories(
        page: Int,
        size: Int,
        sort: GithubSortType,
        language: String?,
    ): Page<GithubRepositoryDto> {
        val pageable = PageRequest.of(page, size)
        return githubRepositoryRepository.findRepositories(pageable, sort, language)
    }

    @Transactional(readOnly = true)
    fun getRepositoryById(id: Long): GithubRepositoryDto {
        return githubRepositoryRepository.findById(id)
            ?: throw GithubRepositoryNotFoundException("GithubRepository with ID $id not found")
    }
}
