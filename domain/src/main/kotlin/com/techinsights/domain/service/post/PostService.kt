package com.techinsights.domain.service.post

import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.enums.Category
import com.techinsights.domain.enums.PostSortType
import com.techinsights.domain.repository.post.PostRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service

@Service
class PostService(
  private val postRepository: PostRepository
) {

  fun getPosts(
    page: Int,
    size: Int,
    sort: PostSortType,
    category: Category,
    companyId: String?
  ): Page<PostDto> {
    val sortSpec = when (sort) {
      PostSortType.RECENT -> Sort.by(Sort.Direction.DESC, "publishedAt")
      PostSortType.POPULAR -> Sort.by(Sort.Direction.DESC, "viewCount")
    }
    val pageable = PageRequest.of(page, size, sortSpec)
    return if (category == Category.All) {
      postRepository.getAllPosts(pageable, companyId)
    } else {
      postRepository.getPostsByCategory(pageable, category, companyId)
    }
  }

  fun getPostById(id: String): PostDto {
    val post = postRepository.getPostById(id)
    return post
  }
}