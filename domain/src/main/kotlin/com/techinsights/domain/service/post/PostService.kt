package com.techinsights.domain.service.post

import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.enums.PostSortType
import com.techinsights.domain.repository.post.PostRepository
import mu.KotlinLogging
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service

@Service
class PostService(
  private val postRepository: PostRepository,
  private val postViewService: PostViewService
) {
  companion object{
    private val logger = KotlinLogging.logger{}
  }


  fun getPosts(page: Int, size: Int, sort: PostSortType): Page<PostDto> {
    val sortSpec = when (sort) {
      PostSortType.RECENT -> Sort.by(Sort.Direction.DESC, "publishedAt")
      PostSortType.POPULAR -> Sort.by(Sort.Direction.DESC, "viewCount")
    }
    val pageable = PageRequest.of(page, size, sortSpec)
    return postRepository.getPosts(pageable)
  }

  fun getPostById(id: String, ip: String): PostDto {
    val post = postRepository.getPostById(id)
    try{
      postViewService.recordView(id, ip)
    }catch (e: Exception) {
      logger.error(e) { "Failed to increment view count for post with id: $id" }
    }
    return post
  }
}