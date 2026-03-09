package com.techinsights.api.github

import com.techinsights.api.post.PageResponse
import com.techinsights.domain.dto.auth.Requester
import com.techinsights.domain.exception.UnauthorizedException
import com.techinsights.domain.service.github.GithubBookmarkService
import org.springframework.data.domain.PageRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/github")
class GithubBookmarkController(
    private val githubBookmarkService: GithubBookmarkService,
) {
    @PostMapping("/{repoId}/bookmark")
    fun toggleBookmark(
        @PathVariable repoId: String,
        requester: Requester,
    ): ResponseEntity<BookmarkResponse> {
        val bookmarked = githubBookmarkService.toggleBookmark(repoId, requester)
        return ResponseEntity.ok(BookmarkResponse(bookmarked))
    }

    @GetMapping("/me/bookmarks")
    fun getMyBookmarks(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        requester: Requester,
    ): ResponseEntity<PageResponse<GithubRepositoryResponse>> {
        val userId = when (requester) {
            is Requester.Authenticated -> requester.userId
            is Requester.Anonymous -> throw UnauthorizedException()
        }
        val result = githubBookmarkService.getMyBookmarks(userId, PageRequest.of(page, size))
        return ResponseEntity.ok(
            PageResponse(
                content = result.content.map { GithubRepositoryResponse.fromDto(it) },
                page = result.number,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
            )
        )
    }

    data class BookmarkResponse(val bookmarked: Boolean)
}
