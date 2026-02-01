package com.techinsights.api.controller.recommend

import com.techinsights.api.response.recommend.PostRecommendResponse
import com.techinsights.domain.dto.auth.Requester
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.service.recommend.RecommendationService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class RecommendController(
  private val recommendationService: RecommendationService
) {

  @GetMapping("/api/v1/recommendations")
  fun getRecommendations(
    requester: Requester
  ): List<PostRecommendResponse> {
    val recommendedPosts: List<PostDto> = recommendationService.getRecommendationsForUser(requester)
    return recommendedPosts.map { PostRecommendResponse.from(it) }
  }

}