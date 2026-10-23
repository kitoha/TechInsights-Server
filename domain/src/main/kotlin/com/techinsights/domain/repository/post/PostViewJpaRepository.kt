package com.techinsights.domain.repository.post

import com.techinsights.domain.entity.post.PostView
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface PostViewJpaRepository : JpaRepository<PostView, Long> {

  @Modifying
  @Query(
    value = """
      INSERT INTO post_view (id, post_id, user_or_ip, viewed_date, user_agent, created_at, updated_at)
      VALUES (:id, :postId, :userOrIp, :viewedDate, :userAgent, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
      ON CONFLICT (post_id, user_or_ip, viewed_date) DO NOTHING
    """,
    nativeQuery = true
  )
  fun insertIfAbsent(
    @Param("id") id: Long,
    @Param("postId") postId: Long,
    @Param("userOrIp") userOrIp: String,
    @Param("viewedDate") viewedDate: LocalDate,
    @Param("userAgent") userAgent: String?
  ): Int
}
