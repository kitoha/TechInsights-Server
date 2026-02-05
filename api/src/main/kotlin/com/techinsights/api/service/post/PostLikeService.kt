package com.techinsights.api.service.post

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
    fun toggleLike(postId: String, userId: Long?, ipAddress: String): Boolean {
        val postIdLong = postId.decode()
        
        val existingLike = if (userId != null) {
            postLikeRepository.findByPostIdAndUserId(postIdLong, userId)
        } else {
            postLikeRepository.findByPostIdAndIpAddress(postIdLong, ipAddress)
        }

        if (existingLike != null) {
            if (userId != null) {
                postLikeRepository.deleteByPostIdAndUserId(postIdLong, userId)
            } else {
                postLikeRepository.deleteByPostIdAndIpAddress(postIdLong, ipAddress)
            }
            postRepository.decrementLikeCount(postIdLong)
            return false
        } else {
            try {
                val newLike = PostLike(
                    id = Tsid.generate().decode(),
                    postId = postIdLong,
                    userId = userId,
                    ipAddress = ipAddress
                )
                postLikeRepository.save(newLike)
                postRepository.incrementLikeCount(postIdLong)
                return true
            } catch (e: DataIntegrityViolationException) {
                return true
            }
        }
    }
}
