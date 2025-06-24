package com.techinsights.domain.dto.company

import com.techinsights.domain.entity.company.Company
import com.techinsights.domain.utils.Tsid

data class CompanyDto(
  val id : String,
  val name: String,
  val blogUrl: String,
  val logoImageName: String,
  val rssSupported: Boolean = false,
  val totalViewCount: Long = 0L,
  val postCount: Long = 0L
){

  fun toEntity(): Company {
    return Company(
      id = Tsid.decode(id),
      name = name,
      blogUrl = blogUrl,
      logoImageName = logoImageName,
      rssSupported = rssSupported,
      totalViewCount = totalViewCount,
      postCount = postCount
    )
  }

  companion object {
    fun fromEntity(entity: Company): CompanyDto {
      return CompanyDto(
        id = Tsid.encode(entity.id),
        name = entity.name,
        blogUrl = entity.blogUrl,
        logoImageName = entity.logoImageName,
        rssSupported = entity.rssSupported,
        totalViewCount = entity.totalViewCount,
        postCount = entity.postCount
      )
    }
  }
}