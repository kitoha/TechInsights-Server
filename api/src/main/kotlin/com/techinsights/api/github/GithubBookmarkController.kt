package com.techinsights.api.github

import com.techinsights.domain.dto.auth.Requester
import com.techinsights.domain.service.github.GithubBookmarkService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/github")
class GithubBookmarkController(
    private val githubBookmarkService: GithubBookmarkService,
) {
    @PostMapping("/{repoId}/bookmark")
    fun toggleBookmark(
        @PathVariable repoId: Long,
        requester: Requester,
    ): ResponseEntity<BookmarkResponse> {
        val bookmarked = githubBookmarkService.toggleBookmark(repoId, requester)
        return ResponseEntity.ok(BookmarkResponse(bookmarked))
    }

    data class BookmarkResponse(val bookmarked: Boolean)
}
