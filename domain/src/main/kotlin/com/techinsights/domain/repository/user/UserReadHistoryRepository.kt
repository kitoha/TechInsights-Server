package com.techinsights.domain.repository.user

import com.techinsights.domain.dto.user.UserReadHistoryDto
import org.springframework.data.domain.Pageable

interface UserReadHistoryRepository {
  fun trackUserPostRead(userId: Long, postId: String)
  fun getRecentReadHistory(
    userId: Long,
    pageable: Pageable
  ): List<UserReadHistoryDto>
}
