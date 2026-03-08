package com.techinsights.domain.service.github

import com.techinsights.domain.entity.github.GithubBookmark
import com.techinsights.domain.repository.github.GithubBookmarkRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class GithubBookmarkSaveHelper(
    private val githubBookmarkRepository: GithubBookmarkRepository,
) {
    private val log = LoggerFactory.getLogger(GithubBookmarkSaveHelper::class.java)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun saveIfAbsent(bookmark: GithubBookmark): Boolean {
        return try {
            githubBookmarkRepository.saveAndFlush(bookmark)
            true
        } catch (e: DataIntegrityViolationException) {
            log.info("Concurrent bookmark detected (idempotent): repoId={}", bookmark.repoId)
            false
        }
    }
}
