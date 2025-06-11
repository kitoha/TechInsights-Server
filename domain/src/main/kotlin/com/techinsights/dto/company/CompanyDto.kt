package com.techinsights.dto.company

import com.techinsights.entity.company.Company
import com.techinsights.utils.Tsid

data class CompanyDto(
  val id : String,
  val name: String,
  val blogUrl: String,
  val logoUrl: String,
  val rssSupported: Boolean = false,
){

  fun toEntity(): Company {
    return Company(
      id = Tsid.decode(id),
      name = name,
      blogUrl = blogUrl,
      logoUrl = logoUrl,
      rssSupported = rssSupported
    )
  }

  companion object {
    fun fromEntity(entity: Company): CompanyDto {
      return CompanyDto(
        id = Tsid.encode(entity.id),
        name = entity.name,
        blogUrl = entity.blogUrl,
        logoUrl = entity.logoUrl,
        rssSupported = entity.rssSupported
      )
    }
  }
}