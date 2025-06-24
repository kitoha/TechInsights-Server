package com.techinsights.domain.dto.post

import com.techinsights.domain.entity.post.PostView
import com.techinsights.domain.utils.Tsid
import java.time.LocalDate
import java.time.LocalDateTime

data class PostViewDto(
    val id: String,
    val postId: String,
    val userOrIp: String,
    val viewedDate: LocalDate,
    val createdAt: LocalDateTime
) {

  fun toEntity(): PostView {
    return PostView(
      id = Tsid.decode(id),
      postId = Tsid.decode(postId),
      userOrIp = userOrIp,
      viewedDate = viewedDate,
      createdAt = createdAt
    )
  }

  companion object{
    fun fromEntity(entity: PostView): PostViewDto {
      return PostViewDto(
        id = entity.id.toString(),
        postId = entity.postId.toString(),
        userOrIp = entity.userOrIp,
        viewedDate = entity.viewedDate,
        createdAt = entity.createdAt
      )
    }
  }
}