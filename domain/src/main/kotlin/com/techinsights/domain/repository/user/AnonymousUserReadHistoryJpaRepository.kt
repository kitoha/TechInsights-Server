package com.techinsights.domain.repository.user

import com.techinsights.domain.entity.user.AnonymousUserReadHistory
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface AnonymousUserReadHistoryJpaRepository : JpaRepository<AnonymousUserReadHistory, Long> {
  fun findTopNByAnonymousIdOrderByReadAtDesc(anonymousId: String, pageable: Pageable): List<AnonymousUserReadHistory>
}