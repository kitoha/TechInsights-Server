package com.techinsights.domain.entity.post

import com.techinsights.domain.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(
    name = "post_likes",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_post_likes_post_user", columnNames = ["post_id", "user_id"]),
        UniqueConstraint(name = "uk_post_likes_post_ip", columnNames = ["post_id", "ip_address"])
    ]
)
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
