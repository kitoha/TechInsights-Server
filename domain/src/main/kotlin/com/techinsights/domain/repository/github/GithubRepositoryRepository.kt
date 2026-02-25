package com.techinsights.domain.repository.github

import com.techinsights.domain.dto.github.GithubRepositoryDto
import com.techinsights.domain.enums.GithubSortType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface GithubRepositoryRepository {
    fun findRepositories(
        pageable: Pageable,
        sortType: GithubSortType,
        language: String?,
    ): Page<GithubRepositoryDto>

    fun findById(id: Long): GithubRepositoryDto?
}
