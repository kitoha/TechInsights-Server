package com.techinsights.domain.entity.post

import com.techinsights.domain.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "post_likes")
class PostLike(
    @Id
    val id: Long,

    @Column(name = "post_id", nullable = false)
    val postId: Long,

    @Column(name = "user_id", nullable = true)
    val userId: Long? = null,

    @Column(name = "ip_address", nullable = false, length = 64)
    val ipAddress: String,

) : BaseEntity()
