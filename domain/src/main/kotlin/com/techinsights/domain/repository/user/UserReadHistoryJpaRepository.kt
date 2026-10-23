package com.techinsights.domain.repository.user

import com.techinsights.domain.entity.user.UserReadHistory
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface UserReadHistoryJpaRepository : JpaRepository<UserReadHistory, Long> {
  fun findByUserIdOrderByReadAtDesc(userId: Long, pageable: Pageable): List<UserReadHistory>
}
