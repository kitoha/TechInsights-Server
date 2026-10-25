package com.techinsights.domain.service.post

import com.techinsights.domain.dto.auth.Requester
import com.techinsights.domain.dto.post.LikeStatusResult
import com.techinsights.domain.entity.post.PostLike
import com.techinsights.domain.exception.PostNotFoundException
import com.techinsights.domain.repository.post.PostLikeRepository
import com.techinsights.domain.repository.post.PostRepository
import com.techinsights.domain.utils.Tsid
import com.techinsights.domain.utils.decode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PostLikeService(
    private val postLikeRepository: PostLikeRepository,
    private val postRepository: PostRepository,
    private val postLikeSaveHelper: PostLikeSaveHelper,
) {
    @Transactional(readOnly = true)
    fun getLikeStatus(postId: String, requester: Requester): LikeStatusResult {
        val postIdLong = postId.decode()
        if (!postRepository.existsById(postIdLong)) {
            throw PostNotFoundException("Post not found: $postId")
        }
        val count = postRepository.getLikeCount(postIdLong)
        val liked = findExistingLike(postIdLong, requester) != null
        return LikeStatusResult(count, liked)
    }

    @Transactional
    fun toggleLike(postId: String, requester: Requester): Boolean {
        val postIdLong = postId.decode()

        if (!postRepository.existsById(postIdLong)) {
            throw PostNotFoundException("Post not found: $postId")
        }

        val existingLike = findExistingLike(postIdLong, requester)

        return if (existingLike != null) {
            handleUnlike(postIdLong, requester)
            false
        } else {
            handleLike(postIdLong, requester)
        }
    }

    /** Idempotent: ensures the like exists. */
    @Transactional
    fun like(postId: String, requester: Requester) {
        val postIdLong = requirePost(postId)

        if (findExistingLike(postIdLong, requester) == null) {
            handleLike(postIdLong, requester)
        }
    }

    /** Idempotent: ensures the like does not exist. */
    @Transactional
    fun unlike(postId: String, requester: Requester) {
        val postIdLong = requirePost(postId)

        handleUnlike(postIdLong, requester)
    }

    private fun requirePost(postId: String): Long {
        val postIdLong = postId.decode()
        if (!postRepository.existsById(postIdLong)) {
            throw PostNotFoundException("Post not found: $postId")
        }
        return postIdLong
    }

    private fun findExistingLike(postId: Long, requester: Requester): PostLike? {
        return when (requester) {
            is Requester.Authenticated -> postLikeRepository.findByPostIdAndUserId(postId, requester.userId)
            is Requester.Anonymous -> postLikeRepository.findAnonymousByPostIdAndAnonymousId(postId, requester.identifier)
        }
    }

    private fun handleUnlike(postId: Long, requester: Requester) {
        val deleted = when (requester) {
            is Requester.Authenticated -> postLikeRepository.deleteByPostIdAndUserId(postId, requester.userId)
            is Requester.Anonymous -> postLikeRepository.deleteAnonymousByPostIdAndAnonymousId(postId, requester.identifier)
        }

        if (deleted > 0L) {
            postRepository.decrementLikeCount(postId)
        }
    }

    private fun handleLike(postId: Long, requester: Requester): Boolean {
        val newLike = PostLike(
            id = Tsid.generateLong(),
            postId = postId,
            userId = if (requester is Requester.Authenticated) requester.userId else null,
            ipAddress = requester.ip,
            anonymousId = if (requester is Requester.Anonymous) requester.identifier else null
        )

        val saved = postLikeSaveHelper.saveIfAbsent(newLike)
        if (saved) {
            postRepository.incrementLikeCount(postId)
        }
        return saved
    }
}
