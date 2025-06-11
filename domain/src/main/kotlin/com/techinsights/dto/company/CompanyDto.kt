package com.techinsights.dto.company

import com.techinsights.entity.company.Company
import com.techinsights.utils.Tsid

data class CompanyDto(
  val id : String,
  val name: String,
  val blogUrl: String,
  val logoImageName: String,
  val rssSupported: Boolean = false,
){

  fun toEntity(): Company {
    return Company(
      id = Tsid.decode(id),
      name = name,
      blogUrl = blogUrl,
      logoImageName = logoImageName,
      rssSupported = rssSupported
    )
  }

  companion object {
    fun fromEntity(entity: Company): CompanyDto {
      return CompanyDto(
        id = Tsid.encode(entity.id),
        name = entity.name,
        blogUrl = entity.blogUrl,
        logoImageName = entity.logoImageName,
        rssSupported = entity.rssSupported
      )
    }
  }
}