package com.techinsights.domain.service.recommend

import com.techinsights.domain.repository.post.PostEmbeddingRepository
import org.springframework.stereotype.Service

@Service
class RecommendationService (
  private val postEmbeddingRepository: PostEmbeddingRepository
){

  /**
   * get recommendations for a user based on their post embeddings.
   * @param userId The ID of the user or Anonymous User for whom recommendations are to be generated.
   * @return A list of recommended posts for the user.
   */
  fun getRecommendationsForUser(userId: Long){

  }
}