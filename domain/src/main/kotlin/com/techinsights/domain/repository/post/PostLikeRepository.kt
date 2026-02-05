package com.techinsights.domain.repository.post

import com.techinsights.domain.entity.post.PostLike
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface PostLikeRepository {
    fun save(postLike: PostLike): PostLike
    fun findByPostIdAndUserId(postId: Long, userId: Long): PostLike?
    fun findByPostIdAndIpAddress(postId: Long, ipAddress: String): PostLike?
    
    fun deleteByPostIdAndUserId(postId: Long, userId: Long)
    fun deleteByPostIdAndIpAddress(postId: Long, ipAddress: String)

    fun findAllByUserId(userId: Long, pageable: Pageable): Page<PostLike>
    fun findAllByIpAddress(ipAddress: String, pageable: Pageable): Page<PostLike>
}
