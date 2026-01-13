package com.techinsights.batch.reader.validator

import com.techinsights.domain.dto.post.PostDto
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object PostValidator {

    private val log: Logger = LoggerFactory.getLogger(PostValidator::class.java)

    private const val MIN_CONTENT_LENGTH = 100

    fun isValidForSummary(post: PostDto): Boolean {
        if (post.content.isBlank()) {
            log.warn("Post ${post.id} has blank content, skipping")
            return false
        }

        if (post.content.length < MIN_CONTENT_LENGTH) {
            log.warn("Post ${post.id} content too short (${post.content.length} chars), skipping")
            return false
        }

        if (post.title.isBlank()) {
            log.warn("Post ${post.id} has blank title, skipping")
            return false
        }

        return true
    }
}
