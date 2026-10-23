package com.techinsights.api.post

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import com.techinsights.api.config.MAX_PAGE_SIZE
import com.techinsights.domain.dto.auth.Requester
import com.techinsights.domain.exception.UnauthorizedException
import com.techinsights.domain.service.post.PostBookmarkService
import org.springframework.data.domain.PageRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/posts")
class PostBookmarkController(
    private val postBookmarkService: PostBookmarkService,
) {
    @Deprecated("Not idempotent (a double click removes the bookmark). Use PUT/DELETE /{postId}/bookmark.")
    @PostMapping("/{postId}/bookmark")
    fun toggleBookmark(
        @PathVariable postId: String,
        requester: Requester,
    ): ResponseEntity<BookmarkResponse> {
        val bookmarked = postBookmarkService.toggleBookmark(postId, requester)
        return ResponseEntity.ok(BookmarkResponse(bookmarked))
    }

    @PutMapping("/{postId}/bookmark")
    fun addBookmark(
        @PathVariable postId: String,
        requester: Requester,
    ): ResponseEntity<BookmarkResponse> {
        postBookmarkService.addBookmark(postId, requester)
        return ResponseEntity.ok(BookmarkResponse(true))
    }

    @DeleteMapping("/{postId}/bookmark")
    fun removeBookmark(
        @PathVariable postId: String,
        requester: Requester,
    ): ResponseEntity<BookmarkResponse> {
        postBookmarkService.removeBookmark(postId, requester)
        return ResponseEntity.ok(BookmarkResponse(false))
    }

    @GetMapping("/me/bookmarks")
    fun getMyBookmarks(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") @Min(1) @Max(MAX_PAGE_SIZE) size: Int,
        requester: Requester,
    ): ResponseEntity<PageResponse<PostResponse>> {
        val userId = when (requester) {
            is Requester.Authenticated -> requester.userId
            is Requester.Anonymous -> throw UnauthorizedException()
        }
        val result = postBookmarkService.getMyBookmarks(userId, PageRequest.of(page, size))
        return ResponseEntity.ok(
            PageResponse(
                content = result.content.map { PostResponse.fromPostDto(it) },
                page = result.number,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
            )
        )
    }

    @GetMapping("/me/bookmarks/count")
    fun countMyBookmarks(requester: Requester): ResponseEntity<CountResponse> {
        val userId = when (requester) {
            is Requester.Authenticated -> requester.userId
            is Requester.Anonymous -> throw UnauthorizedException()
        }
        return ResponseEntity.ok(CountResponse(postBookmarkService.countMyBookmarks(userId)))
    }

    data class BookmarkResponse(val bookmarked: Boolean)
    data class CountResponse(val count: Long)
}
