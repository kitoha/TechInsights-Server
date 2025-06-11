package com.techinsights.api.response

import com.techinsights.dto.company.CompanyDto

data class CompanyResponse(
    val id: String,
    val name: String,
    val blogUrl: String,
    val logoUrl: String
) {
    companion object {
        fun fromDto(dto: CompanyDto): CompanyResponse {
            return CompanyResponse(
                id = dto.id,
                name = dto.name,
                blogUrl = dto.blogUrl,
                logoUrl = dto.logoUrl
            )
        }
    }
}