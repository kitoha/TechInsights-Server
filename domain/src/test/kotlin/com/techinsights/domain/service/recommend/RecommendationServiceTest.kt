package com.techinsights.domain.service.recommend

import com.techinsights.domain.dto.auth.Requester
import com.techinsights.domain.dto.company.CompanyDto
import com.techinsights.domain.dto.embedding.PostEmbeddingDto
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.dto.user.AnonymousUserReadHistoryDto
import com.techinsights.domain.enums.Category
import com.techinsights.domain.repository.post.PostEmbeddingRepository
import com.techinsights.domain.repository.post.PostRepository
import com.techinsights.domain.repository.user.AnonymousUserReadHistoryRepository
import com.techinsights.domain.utils.Tsid
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime

class RecommendationServiceTest : FunSpec({
  val postEmbeddingRepository = mockk<PostEmbeddingRepository>()
  val anonymousUserReadHistoryRepository = mockk<AnonymousUserReadHistoryRepository>()
  val postRepository = mockk<PostRepository>()
  val recommendationService = RecommendationService(
    postEmbeddingRepository,
    anonymousUserReadHistoryRepository,
    postRepository
  )
  val companyId = Tsid.encode(1)
  val postId = Tsid.encode(1)

  val sampleCompanyDto = CompanyDto(
    id = companyId,
    name = "Test Company",
    blogUrl = "http://testcompany.com/blog",
    logoImageName = "logo.png",
    rssSupported = true
  )

  val samplePostDto = PostDto(
    id = postId,
    title = "Test Post",
    preview = "Test preview",
    url = "http://test.com/post1",
    content = "Test content",
    publishedAt = LocalDateTime.now(),
    thumbnail = "thumbnail.png",
    company = sampleCompanyDto,
    viewCount = 10,
    categories = setOf(Category.BackEnd),
    isSummary = true,
    isEmbedding = true
  )

  val sampleEmbeddingDto = PostEmbeddingDto(
    postId = postId,
    companyName = "Test Company",
    categories = "BackEnd",
    content = "Test content",
    embeddingVector = floatArrayOf(0.1f, 0.2f, 0.3f)
  )

  afterTest {
    clearMocks(postRepository, anonymousUserReadHistoryRepository, postEmbeddingRepository)
  }

  test("추천 - 익명 사용자의 읽기 기록이 없는 경우 인기 게시글 반환") {
    val anonymousId = "anonymous-123"
    val requester = Requester.Anonymous(anonymousId, "127.0.0.1")
    val pageRequest = PageRequest.of(0, 10)
    val topPosts = listOf(samplePostDto)

    every {
      anonymousUserReadHistoryRepository.getRecentReadHistory(anonymousId, pageRequest)
    } returns emptyList()
    every { postRepository.findTopViewedPosts(5) } returns topPosts

    val result = recommendationService.getRecommendationsForUser(requester, 5)

    result shouldHaveSize 1
    result[0] shouldBe samplePostDto
    verify(exactly = 1) { postRepository.findTopViewedPosts(5) }
  }

  test("추천 - 인증된 사용자의 읽기 기록이 없는 경우 인기 게시글 반환") {
    val userId = 1L
    val requester = Requester.Authenticated(userId, "127.0.0.1")
    val pageRequest = PageRequest.of(0, 10)
    val topPosts = listOf(samplePostDto)

    every {
      anonymousUserReadHistoryRepository.getRecentReadHistory(userId.toString(), pageRequest)
    } returns emptyList()
    every { postRepository.findTopViewedPosts(5) } returns topPosts

    val result = recommendationService.getRecommendationsForUser(requester, 5)

    result shouldHaveSize 1
    result[0] shouldBe samplePostDto
    verify(exactly = 1) { postRepository.findTopViewedPosts(5) }
  }

  test("추천 - 읽기 기록이 있는 경우 유사 게시글 반환") {
    val anonymousId = "anonymous-123"
    val requester = Requester.Anonymous(anonymousId, "127.0.0.1")
    val pageRequest = PageRequest.of(0, 10)

    val readHistory = listOf(
      AnonymousUserReadHistoryDto(
        id = 1L,
        anonymousId = anonymousId,
        postId = Tsid.encode(1),
        readAt = System.currentTimeMillis()
      )
    )

    val embeddings = listOf(sampleEmbeddingDto)
    val similarEmbeddings = listOf(
      sampleEmbeddingDto.copy(postId = Tsid.encode(2))
    )
    val recommendedPosts = listOf(
      samplePostDto.copy(id = Tsid.encode(2), title = "Recommended Post")
    )

    every {
      anonymousUserReadHistoryRepository.getRecentReadHistory(anonymousId, pageRequest)
    } returns readHistory
    every { postEmbeddingRepository.findByPostIdIn(listOf(1L)) } returns embeddings
    every {
      postEmbeddingRepository.findSimilarPosts(any(), listOf(1L), 5)
    } returns similarEmbeddings
    val recommendedPostId = Tsid.encode(2)
    every { postRepository.findAllByIdIn(listOf(recommendedPostId)) } returns recommendedPosts

    val result = recommendationService.getRecommendationsForUser(requester, 5)

    result shouldHaveSize 1
    result[0].id shouldBe recommendedPostId
    verify(exactly = 1) { postEmbeddingRepository.findSimilarPosts(any(), listOf(1L), 5) }
  }

  test("평균 벡터 계산 - 정상") {
    val vectors = listOf(
      floatArrayOf(1.0f, 2.0f, 3.0f),
      floatArrayOf(2.0f, 4.0f, 6.0f),
      floatArrayOf(3.0f, 6.0f, 9.0f)
    )

    val result = recommendationService.calculateAverageVector(vectors)
    result.size shouldBe 3
    result[0] shouldBe 2.0f
    result[1] shouldBe 4.0f
    result[2] shouldBe 6.0f
  }

  test("평균 벡터 계산 - 빈 리스트") {
    val vectors = emptyList<FloatArray>()

    val result = recommendationService.calculateAverageVector(vectors)

    result.size shouldBe 0
  }

  test("평균 벡터 계산 - 단일 벡터") {
    val vectors = listOf(floatArrayOf(5.0f, 10.0f, 15.0f))

    val result = recommendationService.calculateAverageVector(vectors)

    result.size shouldBe 3
    result[0] shouldBe 5.0f
    result[1] shouldBe 10.0f
    result[2] shouldBe 15.0f
  }

  test("postId 기반 추천 - 유사 게시글이 없는 경우 인기 게시글 반환") {
    val postIds = listOf("1")
    val embeddings = listOf(sampleEmbeddingDto)
    val topPosts = listOf(samplePostDto)

    every { postEmbeddingRepository.findByPostIdIn(listOf(1L)) } returns embeddings
    every {
      postEmbeddingRepository.findSimilarPosts(any(), listOf(1L), 10)
    } returns emptyList()
    every { postRepository.findTopViewedPosts(10) } returns topPosts

    val result = recommendationService.generateRecommendationsFromPostIds(postIds, 10)

    result shouldHaveSize 1
    result[0] shouldBe samplePostDto
    verify(exactly = 1) { postRepository.findTopViewedPosts(10) }
  }
})
