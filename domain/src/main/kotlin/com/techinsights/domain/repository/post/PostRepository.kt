package com.techinsights.domain.repository.post

import com.techinsights.domain.dto.post.PostDto
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface PostRepository {
  fun saveAll(posts: List<PostDto>): List<PostDto>
  fun findAllByUrlIn(urls: List<String>): List<PostDto>
  fun getPosts(pageable: Pageable): Page<PostDto>
  fun getPostById(id: String): PostDto
}