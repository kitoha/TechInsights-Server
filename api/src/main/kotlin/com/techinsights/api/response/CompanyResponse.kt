package com.techinsights.api.response

import com.techinsights.domain.dto.company.CompanyDto

data class CompanyResponse(
    val id: String,
    val name: String,
    val blogUrl: String,
    val logoImageName: String
) {
    companion object {
        fun fromDto(dto: CompanyDto): CompanyResponse {
            return CompanyResponse(
                id = dto.id,
                name = dto.name,
                blogUrl = dto.blogUrl,
                logoImageName = dto.logoImageName
            )
        }
    }
}