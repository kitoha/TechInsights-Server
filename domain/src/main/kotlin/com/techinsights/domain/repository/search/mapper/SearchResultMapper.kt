package com.techinsights.domain.repository.search.mapper

import com.techinsights.domain.dto.search.*
import com.techinsights.domain.entity.company.Company
import com.techinsights.domain.entity.post.Post
import com.techinsights.domain.utils.Tsid
import org.springframework.stereotype.Component

@Component
class SearchResultMapper(
  private val textHighlighter: TextHighlighter
) {

  fun toCompanyMatchDto(
    company: Company,
    matchedPostCount: Long,
    query: String
  ): CompanyMatchDto {
    return CompanyMatchDto(
      id = Tsid.encode(company.id),
      name = company.name,
      logoImageName = company.logoImageName,
      postCount = company.postCount,
      matchedPostCount = matchedPostCount,
      highlightedName = textHighlighter.highlight(company.name, query)
    )
  }

  fun toPostMatchDto(post: Post, query: String): PostMatchDto {
    return PostMatchDto(
      id = Tsid.encode(post.id),
      title = post.title,
      companyName = post.company.name,
      companyLogo = post.company.logoImageName,
      viewCount = post.viewCount,
      publishedAt = post.publishedAt,
      highlightedTitle = textHighlighter.highlight(post.title, query),
      categories = post.categoryValues
    )
  }

  fun toPostSearchResultDto(projection: PostSearchProjection, query: String): PostSearchResultDto {
    val post = projection.post
    return PostSearchResultDto(
      id = Tsid.encode(post.id),
      title = post.title,
      preview = post.preview,
      url = post.url,
      thumbnail = post.thumbnail,
      companyId = Tsid.encode(post.company.id),
      companyName = post.company.name,
      companyLogo = post.company.logoImageName,
      viewCount = post.viewCount,
      publishedAt = post.publishedAt,
      isSummary = post.isSummary,
      categories = post.categoryValues,
      relevanceScore = projection.relevanceScore
    )
  }
}