package com.techinsights.domain.entity.user

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.time.LocalDateTime

@Entity
class AnonymousUserReadHistory(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(name = "anonymous_id", nullable = false, updatable = false)
  val anonymousId: String, // cookie-based identifier for anonymous users

  @Column(name = "post_id", nullable = false, updatable = false)
  val postId: Long,

  @Column(name = "read_at", nullable = false, updatable = false)
  val readAt: LocalDateTime = LocalDateTime.now()
)