package com.techinsights.domain.repository.post

import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.entity.post.PostBookmark
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface PostBookmarkRepository {
    fun saveAndFlush(bookmark: PostBookmark): PostBookmark
    fun findByPostIdAndUserId(postId: Long, userId: Long): PostBookmark?
    fun deleteByPostIdAndUserId(postId: Long, userId: Long): Long
    fun findBookmarkedPosts(userId: Long, pageable: Pageable): Page<PostDto>
}
