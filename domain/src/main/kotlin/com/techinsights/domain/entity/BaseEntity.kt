package com.techinsights.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import org.hibernate.annotations.SQLRestriction
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
@SQLRestriction("deleted_at IS NULL")
abstract class BaseEntity(
  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  var createdAt: LocalDateTime? = null,

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  var updatedAt: LocalDateTime? = null,

  @Column(name = "deleted_at")
  var deletedAt: LocalDateTime? = null
) {

  fun delete() {
    this.deletedAt = LocalDateTime.now()
  }

  val isDeleted: Boolean
    get() = deletedAt != null
}