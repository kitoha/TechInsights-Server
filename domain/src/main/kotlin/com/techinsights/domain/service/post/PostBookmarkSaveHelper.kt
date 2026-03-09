package com.techinsights.domain.service.post

import com.techinsights.domain.entity.post.PostBookmark
import com.techinsights.domain.repository.post.PostBookmarkRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class PostBookmarkSaveHelper(
    private val postBookmarkRepository: PostBookmarkRepository,
) {
    private val log = LoggerFactory.getLogger(PostBookmarkSaveHelper::class.java)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun saveIfAbsent(bookmark: PostBookmark): Boolean {
        return try {
            postBookmarkRepository.saveAndFlush(bookmark)
            true
        } catch (e: DataIntegrityViolationException) {
            log.info("Concurrent bookmark detected (idempotent): postId={}", bookmark.postId)
            false
        }
    }
}
