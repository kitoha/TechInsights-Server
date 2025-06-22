package com.techinsights.domain.service.post

import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.enums.PostSortType
import com.techinsights.domain.repository.post.PostRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service

@Service
class PostService(
  private val postRepository: PostRepository
) {

  fun getPosts(page: Int, size: Int, sort: PostSortType): Page<PostDto> {
    val sortSpec = when (sort) {
      PostSortType.RECENT -> Sort.by(Sort.Direction.DESC, "publishedAt")
      PostSortType.POPULAR -> Sort.by(Sort.Direction.DESC, "viewCount")
    }
    val pageable = PageRequest.of(page, size, sortSpec)
    return postRepository.getPosts(pageable)
  }

  fun getPostById(id: String): PostDto {
    return postRepository.getPostById(id)
  }
}