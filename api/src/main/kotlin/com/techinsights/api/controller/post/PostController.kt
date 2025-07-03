package com.techinsights.api.controller.post

import com.techinsights.api.response.post.PageResponse
import com.techinsights.api.response.post.PostResponse
import com.techinsights.api.util.ClientIpExtractor
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.enums.PostSortType
import com.techinsights.domain.service.post.PostService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class PostController(
  private val postService: PostService,
  private val clientIpExtractor: ClientIpExtractor
) {

  @GetMapping("/api/v1/posts")
  fun getPosts(@RequestParam(defaultValue = "0") page: Int,
    @RequestParam(defaultValue = "10") size: Int,
    @RequestParam(defaultValue = "RECENT") sort: PostSortType
  ): ResponseEntity<PageResponse<PostResponse>> {
    val result = postService.getPosts(page, size, sort)
    val content = result.content.map { PostResponse.fromPostDto(it) }
    return ResponseEntity.ok(
      PageResponse(
      content = content,
      page = result.number,
      size = result.size,
      totalElements = result.totalElements,
      totalPages = result.totalPages
    )
    )
  }

  @GetMapping("/api/v1/posts/{postId}")
  fun getPostById(@PathVariable postId: String,
    request: HttpServletRequest
  ): ResponseEntity<PostResponse> {
    val clientIp = clientIpExtractor.extract(request)
    val postDto: PostDto = postService.getPostById(postId, clientIp)
    return ResponseEntity.ok(PostResponse.fromPostDto(postDto))
  }

}