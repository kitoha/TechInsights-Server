package com.techinsights.domain.dto.user

import com.techinsights.domain.entity.user.UserReadHistory
import java.time.ZoneId

class UserReadHistoryDto(
  val id: Long,
  val userId: Long,
  val postId: String,
  val readAt: Long
) {

  companion object {

    fun fromEntity(entity: UserReadHistory): UserReadHistoryDto {
      return UserReadHistoryDto(
        id = entity.id,
        userId = entity.userId,
        postId = entity.postId.toString(),
        readAt = entity.readAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
      )
    }
  }
}
