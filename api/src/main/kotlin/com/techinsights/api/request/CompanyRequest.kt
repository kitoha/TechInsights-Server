package com.techinsights.api.request

import com.techinsights.dto.company.CompanyDto
import com.techinsights.utils.Tsid

data class CompanyRequest(
  val name: String,
  val blogUrl: String,
  val logoImageName: String,
  val rssSupported: Boolean = false,
){
  fun toDto(): CompanyDto {
    return CompanyDto(
      id = Tsid.generate(),
      name = name,
      blogUrl = blogUrl,
      logoImageName = logoImageName,
      rssSupported = rssSupported
    )
  }
  fun toDtoWithId(id: String): CompanyDto {
    return CompanyDto(
      id = id,
      name = name,
      blogUrl = blogUrl,
      logoImageName = logoImageName,
      rssSupported = rssSupported
    )
  }
}
