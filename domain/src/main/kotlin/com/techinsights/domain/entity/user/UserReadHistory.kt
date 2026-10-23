package com.techinsights.domain.entity.user

import com.techinsights.domain.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "user_read_history")
class UserReadHistory(
  @Id
  val id: Long,

  @Column(name = "user_id", nullable = false, updatable = false)
  val userId: Long,

  @Column(name = "post_id", nullable = false, updatable = false)
  val postId: Long,

  @Column(name = "read_at", nullable = false, updatable = false)
  val readAt: LocalDateTime = LocalDateTime.now()
) : BaseEntity()
