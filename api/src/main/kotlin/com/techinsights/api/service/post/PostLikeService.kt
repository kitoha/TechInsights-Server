package com.techinsights.api.service.post

import com.techinsights.domain.dto.auth.Requester
import com.techinsights.domain.entity.post.PostLike
import com.techinsights.domain.exception.PostNotFoundException
import com.techinsights.domain.repository.post.PostLikeRepository
import com.techinsights.domain.repository.post.PostRepository
import com.techinsights.domain.utils.Tsid
import com.techinsights.domain.utils.decode
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PostLikeService(
    private val postLikeRepository: PostLikeRepository,
    private val postRepository: PostRepository
) {
    private val log = LoggerFactory.getLogger(PostLikeService::class.java)

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
            true
        }
    }

    private fun findExistingLike(postId: Long, requester: Requester): PostLike? {
        return when (requester) {
            is Requester.Authenticated -> postLikeRepository.findByPostIdAndUserId(postId, requester.userId)
            is Requester.Anonymous -> postLikeRepository.findByPostIdAndIpAddress(postId, requester.ip)
        }
    }

    private fun handleUnlike(postId: Long, requester: Requester) {
        val deleted = when (requester) {
            is Requester.Authenticated -> postLikeRepository.deleteByPostIdAndUserId(postId, requester.userId)
            is Requester.Anonymous -> postLikeRepository.deleteByPostIdAndIpAddress(postId, requester.ip)
        }
        
        if (deleted > 0L) {
            postRepository.decrementLikeCount(postId)
        }
    }

    private fun handleLike(postId: Long, requester: Requester) {
        val newLike = PostLike(
            id = Tsid.generateLong(),
            postId = postId,
            userId = if (requester is Requester.Authenticated) requester.userId else null,
            ipAddress = requester.ip
        )

        if (saveLikeIfAbsent(newLike)) {
            postRepository.incrementLikeCount(postId)
        }
    }

    private fun saveLikeIfAbsent(postLike: PostLike): Boolean {
        return try {
            postLikeRepository.save(postLike)
            true
        } catch (e: DataIntegrityViolationException) {
            log.info("Concurrent like detected (idempotent): postId={}, userId={}", 
                postLike.postId, postLike.userId)
            false
        }
    }
}
