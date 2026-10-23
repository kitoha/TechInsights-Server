package com.techinsights.api.search

import com.techinsights.domain.dto.search.InstantSearchResponse as InstantSearchResult

data class InstantSearchResponse(
  val query: String,
  val companies: List<CompanyMatchResponse>,
  val posts: List<PostMatchResponse>
) {

  val isEmpty: Boolean
    get() = companies.isEmpty() && posts.isEmpty()

  companion object {
    fun fromDto(dto: InstantSearchResult): InstantSearchResponse = InstantSearchResponse(
      query = dto.query,
      companies = dto.companies.map { CompanyMatchResponse.fromDto(it) },
      posts = dto.posts.map { PostMatchResponse.fromDto(it) }
    )
  }
}
