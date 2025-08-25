package com.techinsights.api.response.recommend

import com.techinsights.domain.dto.post.PostDto

data class PostRecommendResponse(
  val postId: String,
  val title: String,
  val logoImageName: String,
) {

  companion object {

    fun from(postDto: PostDto): PostRecommendResponse {
      return PostRecommendResponse(
        postId = postDto.id,
        title = postDto.title,
        logoImageName = postDto.company.logoImageName
      )
    }
  }
}