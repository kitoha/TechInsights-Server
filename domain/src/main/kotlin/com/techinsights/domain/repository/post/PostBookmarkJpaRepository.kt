package com.techinsights.domain.repository.post

import com.techinsights.domain.entity.post.PostBookmark
import org.springframework.data.jpa.repository.JpaRepository

interface PostBookmarkJpaRepository : JpaRepository<PostBookmark, Long> {

    fun findByPostIdAndUserId(postId: Long, userId: Long): PostBookmark?

    fun deleteByPostIdAndUserId(postId: Long, userId: Long): Long
}
