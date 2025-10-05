package com.techinsights.api.controller.category

import com.techinsights.api.response.category.CategorySummaryResponse
import com.techinsights.domain.dto.catogory.CategorySummaryDto
import com.techinsights.domain.service.category.CategoryService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class CategoryController(
  private val categoryService: CategoryService
) {

  @GetMapping("/api/v1/categories/summary")
  fun getCategorySummary(): ResponseEntity<List<CategorySummaryResponse>> {
    val summaries: List<CategorySummaryDto> = categoryService.getCategoryStatistics()
    val response = CategorySummaryResponse.from(summaries)
    return ResponseEntity.ok(response)
  }
}