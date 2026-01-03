package com.techinsights.domain.repository.post

import com.techinsights.domain.dto.catogory.CategorySummaryDto
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.enums.Category
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface PostRepository {
  fun saveAll(posts: List<PostDto>): List<PostDto>
  fun findAllByUrlIn(urls: List<String>): List<PostDto>
  fun findAllByIdIn(ids: List<String>): List<PostDto>
  fun getPostById(id: String): PostDto
  fun getCompanyIdByPostId(postId: String): String
  fun findOldestNotSummarized(limit: Long, offset: Long): List<PostDto>
  fun findOldestSummarizedAndNotEmbedded(limit: Long, offset: Long): List<PostDto>
  fun findOldestSummarized(limit: Long, offset: Long): List<PostDto>
  fun findTopViewedPosts(limit: Long): List<PostDto>
  fun getCategoryStatistics(): List<CategorySummaryDto>
  fun getAllPosts(pageable: Pageable, companyId: String?): Page<PostDto>
  fun getPostsByCategory(pageable: Pageable, category: Category, companyId: String?): Page<PostDto>
  fun updateEmbeddingStatusBulk(postIds: List<String>): Long
}