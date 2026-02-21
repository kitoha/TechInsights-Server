package com.techinsights.batch.summary.builder

import com.techinsights.batch.summary.config.BatchBuildConfig
import com.techinsights.domain.dto.post.PostDto
import org.springframework.stereotype.Component

@Component
class PostTruncator(
    private val config: BatchBuildConfig
) {

    fun truncate(post: PostDto, maxTokens: Int): PostDto {
        val maxChars = maxTokens / config.tokensPerChar

        return if (post.content.length > maxChars) {
            post.copy(
                content = post.content.take(maxChars) + TRUNCATION_MARKER
            )
        } else {
            post
        }
    }

    fun calculateMaxTokensForTruncation(): Int {
        return config.maxTokensPerRequest - config.truncationBufferTokens
    }

    companion object {
        private const val TRUNCATION_MARKER = "\n\n[내용이 잘렸습니다]"
    }
}
