package com.techinsights.domain.service.post

import com.techinsights.domain.dto.auth.Requester
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.entity.post.PostBookmark
import com.techinsights.domain.exception.PostNotFoundException
import com.techinsights.domain.exception.UnauthorizedException
import com.techinsights.domain.repository.post.PostBookmarkRepository
import com.techinsights.domain.repository.post.PostRepository
import com.techinsights.domain.utils.Tsid
import com.techinsights.domain.utils.decode
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PostBookmarkService(
    private val postBookmarkRepository: PostBookmarkRepository,
    private val postRepository: PostRepository,
    private val postBookmarkSaveHelper: PostBookmarkSaveHelper,
) {
    @Transactional
    fun toggleBookmark(postId: String, requester: Requester): Boolean {
        val userId = when (requester) {
            is Requester.Authenticated -> requester.userId
            is Requester.Anonymous -> throw UnauthorizedException()
        }
        val postIdLong = postId.decode()

        if (!postRepository.existsById(postIdLong)) {
            throw PostNotFoundException("Post not found: $postId")
        }

        return if (postBookmarkRepository.findByPostIdAndUserId(postIdLong, userId) != null) {
            postBookmarkRepository.deleteByPostIdAndUserId(postIdLong, userId)
            false
        } else {
            postBookmarkSaveHelper.saveIfAbsent(PostBookmark(Tsid.generateLong(), postIdLong, userId))
        }
    }

    @Transactional(readOnly = true)
    fun getMyBookmarks(userId: Long, pageable: Pageable): Page<PostDto> =
        postBookmarkRepository.findBookmarkedPosts(userId, pageable)
}
