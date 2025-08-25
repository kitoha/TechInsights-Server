package com.techinsights.domain.dto.user

import com.techinsights.domain.entity.user.AnonymousUserReadHistory

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
        readAt = entity.readAt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
      )
    }
  }
}