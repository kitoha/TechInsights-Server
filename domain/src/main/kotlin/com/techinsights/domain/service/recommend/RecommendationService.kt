package com.techinsights.domain.service.recommend

import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.repository.post.PostEmbeddingJpaRepository
import com.techinsights.domain.repository.post.PostRepository
import com.techinsights.domain.repository.user.AnonymousUserReadHistoryRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

@Service
class RecommendationService (
  private val postEmbeddingJpaRepository: PostEmbeddingJpaRepository,
  private val anonymousUserReadHistoryRepository: AnonymousUserReadHistoryRepository,
  private val postRepository: PostRepository
){

  /**
   * get recommendations for a user based on their post embeddings.
   * @param userId The ID of the user or Anonymous User for whom recommendations are to be generated.
   * @return A list of recommended posts for the user.
   */
  fun getRecommendationsForUser(anonymousId: String?, count: Long = 10): List<PostDto> {

    if (anonymousId == null) {
      return postRepository.findTopViewedPosts(count)
    }

    val pageRequest = PageRequest.of(0, 10)
    val recentReadHistory = anonymousUserReadHistoryRepository.getRecentReadHistory(
      anonymousId = anonymousId,
      pageable = pageRequest
    )

    val recentPostIds = recentReadHistory.map { it.postId }

    if (recentPostIds.isEmpty()) {
      return postRepository.findTopViewedPosts(count)
    }
    return emptyList()
  }

  fun generateRecommendationsFromPostIds(
    postIds: List<String>,
    count: Long = 10
  ): List<PostDto> {
    if (postIds.isEmpty()) {
      return postRepository.findTopViewedPosts(count)
    }

    return emptyList()
  }
}