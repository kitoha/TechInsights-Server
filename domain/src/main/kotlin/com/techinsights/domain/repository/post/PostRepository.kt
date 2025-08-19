package com.techinsights.domain.repository.post

import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.enums.Category
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface PostRepository {
  fun saveAll(posts: List<PostDto>): List<PostDto>
  fun findAllByUrlIn(urls: List<String>): List<PostDto>
  fun getPosts(pageable: Pageable, category: Category): Page<PostDto>
  fun getPostById(id: String): PostDto
  fun findOldestNotSummarized(limit: Long, offset: Long): List<PostDto>
  fun findOldestSummarized(limit: Long, offset: Long): List<PostDto>
  fun findTopViewedPosts(limit: Long): List<PostDto>
}