package com.techinsights.domain.repository.github

import com.techinsights.domain.dto.github.GithubRepositoryDto
import com.techinsights.domain.enums.ErrorType
import com.techinsights.domain.enums.GithubSortType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime

interface GithubRepositoryRepository {
    fun findRepositories(
        pageable: Pageable,
        sortType: GithubSortType,
        language: String?,
    ): Page<GithubRepositoryDto>

    fun findById(id: Long): GithubRepositoryDto?

    fun findUnsummarized(
        pageSize: Int,
        afterStarCount: Long?,
        afterId: Long?,
        retryAfter: LocalDateTime? = null,
        retryableErrorTypes: Set<ErrorType> = emptySet(),
    ): List<GithubRepositoryDto>

    fun findUnembedded(
        pageSize: Int,
        afterStarCount: Long?,
        afterId: Long?,
    ): List<GithubRepositoryDto>

    fun findSimilarRepositories(
        targetVector: String,
        limit: Long,
    ): List<GithubRepositoryWithDistance>
}
