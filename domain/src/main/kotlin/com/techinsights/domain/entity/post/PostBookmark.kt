package com.techinsights.domain.entity.post

import com.techinsights.domain.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "post_bookmarks")
class PostBookmark(
    @Id val id: Long,
    @Column(name = "post_id", nullable = false) val postId: Long,
    @Column(name = "user_id", nullable = false) val userId: Long,
) : BaseEntity()
