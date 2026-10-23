package com.techinsights.domain.entity.user

import com.techinsights.domain.entity.BaseEntity
import com.techinsights.domain.utils.Tsid
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import java.time.LocalDateTime
import org.hibernate.annotations.SQLRestriction

@Entity
@SQLRestriction("deleted_at IS NULL")
class AnonymousUserReadHistory(
  @Id
  val id: Long = Tsid.generateLong(),

  @Column(name = "anonymous_id", nullable = false, updatable = false)
  val anonymousId: String, // cookie-based identifier for anonymous users

  @Column(name = "post_id", nullable = false, updatable = false)
  val postId: Long,

  @Column(name = "read_at", nullable = false, updatable = false)
  val readAt: LocalDateTime = LocalDateTime.now()
) : BaseEntity()