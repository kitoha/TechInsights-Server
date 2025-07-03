package com.techinsights.api.response.post

import com.techinsights.domain.dto.post.PostDto
import java.time.LocalDateTime

data class PostResponse(
  val id: String,
  val title: String,
  val url: String,
  val content: String,
  val publishedAt: LocalDateTime,
  var thumbnail: String? = null,
  val companyName: String
) {

  companion object{
    fun fromPostDto(postDto : PostDto): PostResponse {
      return PostResponse(
        id = postDto.id,
        title = postDto.title,
        url = postDto.url,
        content = postDto.content,
        publishedAt = postDto.publishedAt,
        thumbnail = postDto.thumbnail,
        companyName = postDto.company.name
      )
    }
  }
}