package com.techinsights.api.service.post

import com.techinsights.domain.dto.auth.Requester
import com.techinsights.domain.entity.post.PostLike
import com.techinsights.domain.repository.post.PostLikeRepository
import com.techinsights.domain.repository.post.PostRepository
import com.techinsights.domain.utils.Tsid
import com.techinsights.domain.utils.decode
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PostLikeService(
    private val postLikeRepository: PostLikeRepository,
    private val postRepository: PostRepository
) {

    @Transactional
    fun toggleLike(postId: String, requester: Requester): Boolean {
        val postIdLong = postId.decode()
        
        return when (requester) {
            is Requester.Authenticated -> toggleAuthenticatedLike(postIdLong, requester.userId, requester.ip)
            is Requester.Anonymous -> toggleAnonymousLike(postIdLong, requester.ip)
        }
    }

    private fun toggleAuthenticatedLike(postId: Long, userId: Long, ipAddress: String): Boolean {
        val existingLike = postLikeRepository.findByPostIdAndUserId(postId, userId)

        if (existingLike != null) {
            postLikeRepository.deleteByPostIdAndUserId(postId, userId)
            postRepository.decrementLikeCount(postId)
            return false
        } else {
            try {
                val newLike = PostLike(
                    id = Tsid.generate().decode(),
                    postId = postId,
                    userId = userId,
                    ipAddress = ipAddress
                )
                postLikeRepository.save(newLike)
                postRepository.incrementLikeCount(postId)
                return true
            } catch (e: DataIntegrityViolationException) {
                return true
            }
        }
    }

    private fun toggleAnonymousLike(postId: Long, ipAddress: String): Boolean {
        val existingLike = postLikeRepository.findByPostIdAndIpAddress(postId, ipAddress)

        if (existingLike != null) {
            postLikeRepository.deleteByPostIdAndIpAddress(postId, ipAddress)
            postRepository.decrementLikeCount(postId)
            return false
        } else {
            try {
                val newLike = PostLike(
                    id = Tsid.generate().decode(),
                    postId = postId,
                    userId = null,
                    ipAddress = ipAddress
                )
                postLikeRepository.save(newLike)
                postRepository.incrementLikeCount(postId)
                return true
            } catch (e: DataIntegrityViolationException) {
                return true
            }
        }
    }
}
