package com.techinsights.domain.service.recommend

import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.repository.post.PostEmbeddingRepository
import com.techinsights.domain.repository.post.PostRepository
import com.techinsights.domain.repository.user.AnonymousUserReadHistoryRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

@Service
class RecommendationService(
  private val postEmbeddingRepository: PostEmbeddingRepository,
  private val anonymousUserReadHistoryRepository: AnonymousUserReadHistoryRepository,
  private val postRepository: PostRepository
) {

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

    val postEmbeddings = postEmbeddingRepository.findByPostIdIn(recentPostIds)

    if (postEmbeddings.isEmpty()) {
      return postRepository.findTopViewedPosts(count)
    }




    return emptyList()
  }

  fun generateRecommendationsFromPostIds(
    postIds: List<String>,
    count: Long = 10
  ): List<PostDto> {

    val postIdsLong = postIds.map { it.toLongOrNull() ?: return emptyList() }
    val recentEmbeddings = postEmbeddingRepository.findByPostIdIn(postIdsLong)
    val vectors = recentEmbeddings.map { it.embeddingVector }

    val averageVector = calculateAverageVector(vectors)

    val vectorString = averageVector.joinToString(prefix = "[", postfix = "]")

    val embeddingDto = postEmbeddingRepository.findSimilarPosts(vectorString, postIdsLong, count)

    if (embeddingDto.isEmpty()) {
      return postRepository.findTopViewedPosts(count)
    }

    val recommendedPostIds = embeddingDto.map { it.postId }

    return postRepository.findAllByIdIn(recommendedPostIds)
  }

  fun calculateAverageVector(vectors: List<FloatArray>): FloatArray {
    if (vectors.isEmpty()) return floatArrayOf()

    val vectorSize = vectors[0].size
    val averageVector = FloatArray(vectorSize) { 0.0f }

    for (vector in vectors) {
      if (vector.size == vectorSize) {
        for (i in vector.indices) {
          averageVector[i] += vector[i]
        }
      }
    }

    val vectorCount = vectors.size
    for (i in averageVector.indices) {
      averageVector[i] /= vectorCount
    }

    return averageVector
  }
}