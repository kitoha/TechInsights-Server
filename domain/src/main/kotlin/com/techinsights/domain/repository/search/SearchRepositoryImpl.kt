package com.techinsights.domain.repository.search

import com.techinsights.domain.config.search.SearchProperties
import com.techinsights.domain.dto.search.CompanyMatchDto
import com.techinsights.domain.dto.search.InstantSearchResponse
import com.techinsights.domain.dto.search.PostMatchDto
import com.techinsights.domain.dto.search.PostSearchResultDto
import com.techinsights.domain.dto.search.SearchRequest
import com.techinsights.domain.repository.search.mapper.SearchResultMapper
import com.techinsights.domain.repository.search.query.SearchQueryBuilder
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository

@Repository
class SearchRepositoryImpl(
  private val postSearchQueryRepository: PostSearchQueryRepository,
  private val companySearchQueryRepository: CompanySearchQueryRepository,
  private val queryBuilder: SearchQueryBuilder,
  private val resultMapper: SearchResultMapper,
  private val searchProperties: SearchProperties
) : SearchRepository {

  override fun instantSearch(query: String, limit: Int): InstantSearchResponse {
    if (query.isBlank()) {
      return InstantSearchResponse(query, emptyList(), emptyList())
    }

    val companies = findCompanies(query, limit)
    val posts = findPosts(query, limit)

    return InstantSearchResponse(
      query = query,
      companies = companies,
      posts = posts
    )
  }

  override fun fullSearch(request: SearchRequest): Page<PostSearchResultDto> {
    if (request.query.isBlank()) {
      return PageImpl(emptyList(), PageRequest.of(request.page, request.size), 0)
    }

    val relevanceScore = queryBuilder.buildRelevanceScore(request.query)
    val searchCondition = queryBuilder.buildSearchCondition(request.query, request.companyId)
    val orderSpecifiers = queryBuilder.buildOrderSpecifier(request.sortBy, relevanceScore)

    val posts = postSearchQueryRepository.findForFullSearch(
      condition = searchCondition,
      orderSpecifiers = orderSpecifiers,
      offset = (request.page * request.size).toLong(),
      limit = request.size.toLong()
    )

    val total = postSearchQueryRepository.countByCondition(searchCondition)
    val dtos = posts.map { resultMapper.toPostSearchResultDto(it, request.query) }

    return PageImpl(dtos, PageRequest.of(request.page, request.size), total)
  }

  private fun findCompanies(query: String, limit: Int): List<CompanyMatchDto> {
    val companies = companySearchQueryRepository.findByNameContaining(query, limit)

    return companies.map { company ->
      val matchedPostCount = postSearchQueryRepository
        .countMatchedPostsByCompany(company.id, query)

      resultMapper.toCompanyMatchDto(company, matchedPostCount, query)
    }
  }

  private fun findPosts(query: String, limit: Int): List<PostMatchDto> {
    val similarityScore = queryBuilder.buildSimilarityScore(query)
    val posts = postSearchQueryRepository.findForInstantSearch(
      query = query,
      similarityScore = similarityScore,
      limit = limit
    )

    return posts.map { resultMapper.toPostMatchDto(it, query) }
  }
}