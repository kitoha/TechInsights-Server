package com.techinsights.domain.repository.post

import com.techinsights.domain.entity.post.PostLike
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface PostLikeJpaRepository : JpaRepository<PostLike, Long> {
    fun findByPostIdAndUserId(postId: Long, userId: Long): PostLike?
    fun findByPostIdAndIpAddress(postId: Long, ipAddress: String): PostLike?
    
    fun deleteByPostIdAndUserId(postId: Long, userId: Long): Long
    fun deleteByPostIdAndIpAddress(postId: Long, ipAddress: String): Long

    fun findAllByUserId(userId: Long, pageable: Pageable): Page<PostLike>
    fun findAllByIpAddress(ipAddress: String, pageable: Pageable): Page<PostLike>
}
