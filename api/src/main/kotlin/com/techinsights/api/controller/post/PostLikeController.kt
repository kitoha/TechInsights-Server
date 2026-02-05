package com.techinsights.api.controller.post

import com.techinsights.api.service.post.PostLikeService
import com.techinsights.api.util.ClientIpExtractor
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/posts")
class PostLikeController(
    private val postLikeService: PostLikeService,
    private val clientIpExtractor: ClientIpExtractor
) {

    @PostMapping("/{postId}/like")
    fun toggleLike(
        @PathVariable postId: String,
        @RequestHeader(value = "X-User-Id", required = false) userId: Long?,
        request: HttpServletRequest
    ): ResponseEntity<LikeResponse> {
        val clientIp = clientIpExtractor.extract(request)
        val liked = postLikeService.toggleLike(postId, userId, clientIp)
        
        return ResponseEntity.ok(LikeResponse(liked))
    }

    data class LikeResponse(val liked: Boolean)
}
