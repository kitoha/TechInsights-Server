package com.techinsights.domain.dto.search

import com.techinsights.domain.enums.search.SearchSortType

data class SearchRequest(
  val query: String,
  val page: Int = 0,
  val size: Int = 20,
  val sortBy: SearchSortType = SearchSortType.RELEVANCE,
  val companyId: Long? = null
) {

  fun validate(maxSize: Int): SearchRequest {
    require(query.isNotBlank()) { "검색어는 필수입니다" }
    require(page >= 0) { "페이지는 0 이상이어야 합니다" }
    require(size in 1..maxSize) { "페이지 크기는 1~$maxSize 사이여야 합니다" }
    return this
  }
}