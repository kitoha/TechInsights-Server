package com.techinsights.domain.dto.search

data class InstantSearchResponse(
  val query: String,
  val companies: List<CompanyMatchDto>,
  val posts: List<PostMatchDto>
) {

  val isEmpty: Boolean
    get() = companies.isEmpty() && posts.isEmpty()
}