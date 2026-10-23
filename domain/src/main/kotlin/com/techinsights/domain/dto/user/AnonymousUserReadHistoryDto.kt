package com.techinsights.domain.dto.user

import com.techinsights.domain.entity.user.AnonymousUserReadHistory
import java.time.ZoneId

class AnonymousUserReadHistoryDto(
  val id: Long?,
  val anonymousId: String,
  val postId: String,
  val readAt: Long
) {

  companion object {

    fun fromEntity(entity: AnonymousUserReadHistory): AnonymousUserReadHistoryDto {
      return AnonymousUserReadHistoryDto(
        id = entity.id,
        anonymousId = entity.anonymousId,
        postId = entity.postId.toString(),
        readAt = entity.readAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
      )
    }
  }
}