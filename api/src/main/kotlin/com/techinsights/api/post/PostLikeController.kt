package com.techinsights.api.post

import com.techinsights.domain.dto.auth.Requester
import com.techinsights.domain.service.post.PostLikeService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/posts")
class PostLikeController(
    private val postLikeService: PostLikeService
) {

    @GetMapping("/{postId}/like")
    fun getLikeStatus(
        @PathVariable postId: String,
        requester: Requester
    ): ResponseEntity<LikeStatusResponse> {
        val result = postLikeService.getLikeStatus(postId, requester)
        return ResponseEntity.ok(LikeStatusResponse(result.count, result.liked))
    }

    @PostMapping("/{postId}/like")
    fun toggleLike(
        @PathVariable postId: String,
        requester: Requester
    ): ResponseEntity<LikeResponse> {
        val liked = postLikeService.toggleLike(postId, requester)
        return ResponseEntity.ok(LikeResponse(liked))
    }

    data class LikeResponse(val liked: Boolean)
    data class LikeStatusResponse(val count: Long, val liked: Boolean)
}
