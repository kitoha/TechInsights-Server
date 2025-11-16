package com.techinsights.domain.repository.search

import com.techinsights.domain.dto.search.*
import com.techinsights.domain.entity.company.Company
import com.techinsights.domain.entity.post.Post
import com.techinsights.domain.repository.search.mapper.SearchResultMapper
import com.techinsights.domain.repository.search.query.SearchQueryBuilder
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.core.types.dsl.NumberExpression
import com.techinsights.domain.enums.search.SearchSortType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import io.mockk.*

class SearchRepositoryImplTest : FunSpec({

  val postSearchQueryRepository = mockk<PostSearchQueryRepository>()
  val companySearchQueryRepository = mockk<CompanySearchQueryRepository>()
  val queryBuilder = mockk<SearchQueryBuilder>()
  val resultMapper = mockk<SearchResultMapper>()

  val repository = SearchRepositoryImpl(
    postSearchQueryRepository,
    companySearchQueryRepository,
    queryBuilder,
    resultMapper
  )

  beforeTest {
    clearMocks(
      postSearchQueryRepository,
      companySearchQueryRepository,
      queryBuilder,
      resultMapper
    )
  }

  test("instantSearch should return empty response for blank query") {
    val query = " "
    val limit = 5

    val result = repository.instantSearch(query, limit)

    result.query shouldBe query
    result.companies shouldBe emptyList()
    result.posts shouldBe emptyList()

    verify(exactly = 0) { companySearchQueryRepository.findByNameContaining(any(), any()) }
    verify(exactly = 0) { postSearchQueryRepository.findForInstantSearch(any(), any(), any()) }
  }

  test("instantSearch should return matched companies and posts") {
    val query = "tech"
    val limit = 3
    val companyId = 1L
    val matchedPostCount = 2L

    val mockCompany = mockk<Company>()
    val mockPost = mockk<Post>()
    val mockRelevanceScore = mockk<NumberExpression<Double>>()
    val mockProjection = mockk<PostSearchProjection> {
      every { post } returns mockPost
      every { relevanceScore } returns 0.9
    }

    val mockCompanyDto = mockk<CompanyMatchDto>()
    val mockPostDto = mockk<PostMatchDto>()

    every { mockCompany.id } returns companyId
    every { queryBuilder.buildRelevanceScore(query) } returns mockRelevanceScore
    every { companySearchQueryRepository.findByNameContaining(query, limit) } returns listOf(
      mockCompany
    )
    every {
      postSearchQueryRepository.countMatchedPostsByCompany(
        companyId,
        query
      )
    } returns matchedPostCount
    every {
      resultMapper.toCompanyMatchDto(
        mockCompany,
        matchedPostCount,
        query
      )
    } returns mockCompanyDto
    every {
      postSearchQueryRepository.findForInstantSearch(
        query,
        mockRelevanceScore,
        limit
      )
    } returns listOf(mockProjection)
    every { resultMapper.toPostMatchDto(mockPost, query) } returns mockPostDto

    val result = repository.instantSearch(query, limit)

    result.query shouldBe query
    result.companies shouldBe listOf(mockCompanyDto)
    result.posts shouldBe listOf(mockPostDto)

    verifyAll {
      queryBuilder.buildRelevanceScore(query)
      companySearchQueryRepository.findByNameContaining(query, limit)
      postSearchQueryRepository.countMatchedPostsByCompany(companyId, query)
      resultMapper.toCompanyMatchDto(mockCompany, matchedPostCount, query)
      postSearchQueryRepository.findForInstantSearch(query, mockRelevanceScore, limit)
      resultMapper.toPostMatchDto(mockPost, query)
    }
  }

  test("fullSearch should return empty page for blank query") {
    val request = SearchRequest(
      query = " ",
      page = 0,
      size = 10,
      sortBy = SearchSortType.RELEVANCE,
      companyId = null
    )

    val result = repository.fullSearch(request)

    result.shouldBeInstanceOf<PageImpl<PostSearchResultDto>>()
    result.totalElements shouldBe 0L
    result.content shouldBe emptyList()

    verify(exactly = 0) { queryBuilder.buildRelevanceScore(any()) }
    verify(exactly = 0) {
      postSearchQueryRepository.findForFullSearch(
        any(),
        any(),
        any(),
        any(),
        any()
      )
    }
    verify(exactly = 0) { postSearchQueryRepository.countByCondition(any()) }
  }

  test("fullSearch should return paginated search results") {
    val request = SearchRequest(
      query = "test",
      page = 0,
      size = 10,
      sortBy = SearchSortType.RELEVANCE,
      companyId = 123L
    )
    val totalElements = 15L
    val offset = 0L
    val limit = 10L

    val mockRelevanceScore = mockk<NumberExpression<Double>>()
    val mockCondition = mockk<BooleanExpression>()
    val mockOrder = mockk<OrderSpecifier<*>>()
    val mockOrderSpecifiers = arrayOf(mockOrder)
    val mockProjection = mockk<PostSearchProjection>()
    val mockResultDto = mockk<PostSearchResultDto>()

    every { queryBuilder.buildRelevanceScore(request.query) } returns mockRelevanceScore
    every {
      queryBuilder.buildSearchCondition(
        request.query,
        request.companyId
      )
    } returns mockCondition
    every {
      queryBuilder.buildOrderSpecifier(
        request.sortBy,
        mockRelevanceScore
      )
    } returns mockOrderSpecifiers
    every {
      postSearchQueryRepository.findForFullSearch(
        condition = mockCondition,
        orderSpecifiers = mockOrderSpecifiers,
        relevanceScore = mockRelevanceScore,
        offset = offset,
        limit = limit
      )
    } returns listOf(mockProjection)
    every { postSearchQueryRepository.countByCondition(mockCondition) } returns totalElements
    every {
      resultMapper.toPostSearchResultDto(
        mockProjection,
        request.query
      )
    } returns mockResultDto

    val result = repository.fullSearch(request)

    result.shouldBeInstanceOf<Page<PostSearchResultDto>>()
    result.totalElements shouldBe totalElements
    result.totalPages shouldBe 2
    result.content shouldBe listOf(mockResultDto)
    result.pageable.pageNumber shouldBe request.page
    result.pageable.pageSize shouldBe request.size

    verifyAll {
      queryBuilder.buildRelevanceScore(request.query)
      queryBuilder.buildSearchCondition(request.query, request.companyId)
      queryBuilder.buildOrderSpecifier(request.sortBy, mockRelevanceScore)
      postSearchQueryRepository.findForFullSearch(
        condition = mockCondition,
        orderSpecifiers = mockOrderSpecifiers,
        relevanceScore = mockRelevanceScore,
        offset = offset,
        limit = limit
      )
      postSearchQueryRepository.countByCondition(mockCondition)
      resultMapper.toPostSearchResultDto(mockProjection, request.query)
    }
  }
})