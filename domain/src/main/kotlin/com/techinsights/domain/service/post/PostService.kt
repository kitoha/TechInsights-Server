package com.techinsights.domain.service.post

import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.enums.Category
import com.techinsights.domain.enums.PostSortType
import com.techinsights.domain.repository.post.PostRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PostService(
  private val postRepository: PostRepository
) {

  @Transactional(readOnly = true)
  fun getPosts(
    page: Int,
    size: Int,
    sort: PostSortType,
    category: Category,
    companyId: String?
  ): Page<PostDto> {
    val pageable = PageRequest.of(page, size)
    return if (category == Category.All) {
      postRepository.getAllPosts(pageable, companyId, sort)
    } else {
      postRepository.getPostsByCategory(pageable, category, companyId, sort)
    }
  }

  @Transactional(readOnly = true)
  fun getPostById(id: String): PostDto {
    val post = postRepository.getPostById(id)
    return post
  }
}