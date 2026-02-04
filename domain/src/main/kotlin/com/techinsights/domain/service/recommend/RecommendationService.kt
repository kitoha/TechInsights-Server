package com.techinsights.domain.service.recommend

import com.techinsights.domain.dto.auth.Requester
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
   * @param requester The requester (Authenticated or Anonymous) for whom recommendations are to be generated.
   * @return A list of recommended posts for the user.
   */
  fun getRecommendationsForUser(requester: Requester, count: Long = 5): List<PostDto> {

    val identifier = requester.identifier

    val pageRequest = PageRequest.of(0, 10)
    
    // TODO: If requester is Authenticated, we should use UserReadHistoryRepository
    val recentReadHistory = anonymousUserReadHistoryRepository.getRecentReadHistory(
      anonymousId = identifier,
      pageable = pageRequest
    )

    val recentPostIds = recentReadHistory.map { it.postId }

    if (recentPostIds.isEmpty()) {
      return postRepository.findTopViewedPosts(count)
    }

    return generateRecommendationsFromPostIds(recentPostIds.map { it.toString() }, count)
  }

  fun generateRecommendationsFromPostIds(
    postIds: List<String>,
    count: Long = 10
  ): List<PostDto> {

    val postIdsLong = postIds.map { it.toLongOrNull() ?: return emptyList() }
    val recentEmbeddings = postEmbeddingRepository.findByPostIdIn(postIdsLong)
    val vectors = recentEmbeddings.map { it.embeddingVector }

    if (vectors.isEmpty()) {
      return postRepository.findTopViewedPosts(count)
    }

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
          averageVector[i] = averageVector[i] + vector[i]
        }
      }
    }

    val vectorCount = vectors.size
    for (i in averageVector.indices) {
      averageVector[i] = averageVector[i] / vectorCount.toFloat()
    }

    return averageVector
  }
}