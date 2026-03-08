package com.techinsights.api.post

import com.techinsights.domain.dto.auth.Requester
import com.techinsights.domain.service.post.PostBookmarkService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/posts")
class PostBookmarkController(
    private val postBookmarkService: PostBookmarkService,
) {
    @PostMapping("/{postId}/bookmark")
    fun toggleBookmark(
        @PathVariable postId: String,
        requester: Requester,
    ): ResponseEntity<BookmarkResponse> {
        val bookmarked = postBookmarkService.toggleBookmark(postId, requester)
        return ResponseEntity.ok(BookmarkResponse(bookmarked))
    }

    data class BookmarkResponse(val bookmarked: Boolean)
}
