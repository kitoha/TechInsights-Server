package com.techinsights.domain.repository.user

import com.techinsights.domain.entity.user.AnonymousUserReadHistory
import org.springframework.data.domain.Pageable

interface AnonymousUserReadHistoryRepository {
  fun trackAnonymousPostRead(anonymousId: String, postId: String)
  fun getRecentReadHistory(anonymousId: String, pageable: Pageable): List<AnonymousUserReadHistory>
}