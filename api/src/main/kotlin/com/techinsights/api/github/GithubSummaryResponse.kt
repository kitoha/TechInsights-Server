package com.techinsights.api.github

import com.techinsights.domain.dto.github.GithubSummaryDto

data class GithubSummaryResponse(
    val totalRepositories: Long,
    val totalStars: Long
) {

    companion object {
        fun fromDto(dto: GithubSummaryDto): GithubSummaryResponse = GithubSummaryResponse(
            totalRepositories = dto.totalRepositories,
            totalStars = dto.totalStars
        )
    }
}
