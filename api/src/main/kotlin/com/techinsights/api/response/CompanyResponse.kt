package com.techinsights.api.response

import com.techinsights.domain.dto.company.CompanyDto

data class CompanyResponse(
    val id: String,
    val name: String,
    val blogUrl: String,
    val logoImageName: String,
    val totalViewCount: Long = 0L,
    val postCount: Long = 0L
) {
    companion object {
        fun fromDto(dto: CompanyDto): CompanyResponse {
            return CompanyResponse(
                id = dto.id,
                name = dto.name,
                blogUrl = dto.blogUrl,
                logoImageName = dto.logoImageName,
                totalViewCount = dto.totalViewCount,
                postCount = dto.postCount
            )
        }
    }
}