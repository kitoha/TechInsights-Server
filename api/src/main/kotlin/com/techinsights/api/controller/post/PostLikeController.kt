package com.techinsights.api.controller.post

import com.techinsights.api.service.post.PostLikeService
import com.techinsights.domain.dto.auth.Requester
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/posts")
class PostLikeController(
    private val postLikeService: PostLikeService
) {

    @PostMapping("/{postId}/like")
    fun toggleLike(
        @PathVariable postId: String,
        requester: Requester
    ): ResponseEntity<LikeResponse> {
        val liked = postLikeService.toggleLike(postId, requester)
        return ResponseEntity.ok(LikeResponse(liked))
    }

    data class LikeResponse(val liked: Boolean)
}
