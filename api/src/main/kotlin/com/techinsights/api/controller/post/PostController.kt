package com.techinsights.api.controller.post

import com.techinsights.api.response.post.PageResponse
import com.techinsights.api.response.post.PostResponse
import com.techinsights.domain.dto.auth.Requester
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.enums.Category
import com.techinsights.domain.enums.PostSortType
import com.techinsights.domain.repository.user.AnonymousUserReadHistoryRepository
import com.techinsights.domain.service.post.PostService
import com.techinsights.domain.service.post.PostViewService
import jakarta.servlet.http.HttpServletRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class PostController(
  private val postService: PostService,
  private val postViewService: PostViewService,
  private val anonymousUserReadHistoryRepository: AnonymousUserReadHistoryRepository,
  @param:Qualifier("ioDispatcher") private val ioDispatcher: CoroutineDispatcher
) {

  @GetMapping("/api/v1/posts")
  suspend fun getPosts(@RequestParam(defaultValue = "0") page: Int,
    @RequestParam(defaultValue = "10") size: Int,
    @RequestParam(defaultValue = "RECENT") sort: PostSortType,
    @RequestParam(defaultValue = "All") category: Category,
    @RequestParam(required = false) companyId: String?
  ): ResponseEntity<PageResponse<PostResponse>> = withContext(ioDispatcher) {
    val result = postService.getPosts(page, size, sort, category, companyId)
    val content = result.content.map { PostResponse.fromPostDto(it) }
    ResponseEntity.ok(
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
  suspend fun getPostById(@PathVariable postId: String): ResponseEntity<PostResponse> = withContext(ioDispatcher) {
    val postDto: PostDto = postService.getPostById(postId)
    ResponseEntity.ok(PostResponse.fromPostDto(postDto))
  }

  @PostMapping("/api/v1/posts/{postId}/view")
  fun recordView(
    @PathVariable postId: String,
    requester: Requester,
    request: HttpServletRequest
  ): ResponseEntity<Unit> {
    val userAgent = request.getHeader("User-Agent")

    val identifier = when (requester) {
      is Requester.Authenticated -> "U_${requester.userId}"
      is Requester.Anonymous -> requester.ip
    }

    postViewService.recordView(postId, identifier, userAgent)

    if (requester is Requester.Anonymous) {
      anonymousUserReadHistoryRepository.trackAnonymousPostRead(requester.ip, postId)
    }

    return ResponseEntity.ok().build()
  }

}
