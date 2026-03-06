package com.techinsights.domain.service.post

import com.techinsights.domain.entity.post.PostLike
import com.techinsights.domain.repository.post.PostLikeRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class PostLikeSaveHelper(
    private val postLikeRepository: PostLikeRepository,
) {
    private val log = LoggerFactory.getLogger(PostLikeSaveHelper::class.java)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun saveIfAbsent(postLike: PostLike): Boolean {
        return try {
            postLikeRepository.save(postLike)
            true
        } catch (e: DataIntegrityViolationException) {
            log.info(
                "Concurrent like detected (idempotent): postId={}, userId={}, ip={}",
                postLike.postId, postLike.userId, postLike.ipAddress
            )
            false
        }
    }
}
