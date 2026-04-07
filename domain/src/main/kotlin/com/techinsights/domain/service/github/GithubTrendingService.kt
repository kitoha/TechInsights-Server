package com.techinsights.domain.service.github

import com.techinsights.domain.config.cache.CacheConfig
import com.techinsights.domain.dto.github.GithubRepositoryDto
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
