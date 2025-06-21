package com.techinsights.api.controller.post

import com.techinsights.api.response.PageResponse
import com.techinsights.api.response.PostResponse
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.enums.PostSortType
import com.techinsights.domain.service.post.PostService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class PostController(
  private val postService: PostService
) {

  @GetMapping("/api/v1/posts")
  fun getPosts(@RequestParam(defaultValue = "0") page: Int,
    @RequestParam(defaultValue = "10") size: Int,
    @RequestParam(defaultValue = "RECENT") sort: PostSortType
  ): PageResponse<PostResponse> {
    val result = postService.getPosts(page, size, sort)
    val content = result.content.map { PostResponse.fromPostDto(it) }
    return PageResponse(
      content = content,
      page = result.number,
      size = result.size,
      totalElements = result.totalElements,
      totalPages = result.totalPages
    )
  }

  @GetMapping("/api/v1/posts/{id}")
  fun getPostById(id: String): ResponseEntity<PostResponse> {
    val postDto: PostDto = postService.getPostById(id)
    return ResponseEntity.ok(PostResponse.fromPostDto(postDto))
  }

}