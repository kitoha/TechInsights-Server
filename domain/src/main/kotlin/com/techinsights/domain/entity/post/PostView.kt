package com.techinsights.domain.entity.post

import com.techinsights.domain.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(name = "post_view")
class PostView (
  @Id
  val id: Long,
  @Column(name = "post_id", nullable = false)
  val postId: Long,

  @Column(name = "user_or_ip", nullable = false, length = 64)
  val userOrIp: String,

  @Column(name = "viewed_date", nullable = false)
  val viewedDate: LocalDate,

  ) : BaseEntity()