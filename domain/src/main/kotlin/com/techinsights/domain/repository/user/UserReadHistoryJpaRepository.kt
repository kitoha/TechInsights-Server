package com.techinsights.domain.repository.user

import com.techinsights.domain.entity.user.UserReadHistory
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface UserReadHistoryJpaRepository : JpaRepository<UserReadHistory, Long> {
  fun findByUserIdOrderByReadAtDesc(userId: Long, pageable: Pageable): List<UserReadHistory>

  @Modifying
  @Query(
    value = """
      INSERT INTO user_read_history (id, user_id, post_id, read_at, created_at, updated_at)
      VALUES (:id, :userId, :postId, :readAt, :readAt, :readAt)
      ON CONFLICT (user_id, post_id) DO UPDATE
        SET read_at = EXCLUDED.read_at, updated_at = EXCLUDED.updated_at, deleted_at = NULL
    """,
    nativeQuery = true
  )
  fun upsertRead(
    @Param("id") id: Long,
    @Param("userId") userId: Long,
    @Param("postId") postId: Long,
    @Param("readAt") readAt: LocalDateTime
  ): Int
}
