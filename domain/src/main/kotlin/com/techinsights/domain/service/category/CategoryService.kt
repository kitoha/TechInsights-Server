package com.techinsights.domain.service.category

import com.techinsights.domain.dto.catogory.CategorySummaryDto
import com.techinsights.domain.repository.post.PostRepository
import org.springframework.stereotype.Service

@Service
class CategoryService(
  private val postRepository: PostRepository
) {

  fun getCategoryStatistics(): List<CategorySummaryDto> {
    return postRepository.getCategoryStatistics()
  }

}