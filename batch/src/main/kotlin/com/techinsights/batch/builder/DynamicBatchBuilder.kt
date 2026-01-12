package com.techinsights.batch.builder

import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.utils.TokenEstimator
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class DynamicBatchBuilder(
    private val maxTokensPerRequest: Int = 200_000,  // TPM의 80%만 사용 (안전 마진)
    private val minBatchSize: Int = 1,
    private val maxBatchSize: Int = 20
) {

    private val log = LoggerFactory.getLogger(DynamicBatchBuilder::class.java)

    data class Batch(
        val items: List<PostDto>,
        val estimatedTokens: Int
    )

    fun buildBatches(posts: List<PostDto>): List<Batch> {
        val batches = mutableListOf<Batch>()
        var currentBatch = mutableListOf<PostDto>()
        var currentTokens = 500  // 기본 프롬프트 토큰

        for (post in posts) {
            val postTotalTokens = TokenEstimator.estimateTotalTokens(post.content)

            if (postTotalTokens > maxTokensPerRequest) {
                log.warn("Post ${post.id} too large (${postTotalTokens} tokens), truncating")
                val truncatedPost = truncatePost(post, maxTokensPerRequest - 2000)

                if (currentBatch.isNotEmpty()) {
                    batches.add(Batch(currentBatch.toList(), currentTokens))
                    currentBatch.clear()
                    currentTokens = 500
                }

                batches.add(Batch(listOf(truncatedPost), TokenEstimator.estimateTotalTokens(truncatedPost.content)))
                continue
            }

            if ((currentTokens + postTotalTokens > maxTokensPerRequest ||
                currentBatch.size >= maxBatchSize) && currentBatch.isNotEmpty()) {
                batches.add(Batch(currentBatch.toList(), currentTokens))
                currentBatch.clear()
                currentTokens = 500
            }

            currentBatch.add(post)
            currentTokens += postTotalTokens
        }

        if (currentBatch.isNotEmpty()) {
            batches.add(Batch(currentBatch.toList(), currentTokens))
        }

        log.info("Built ${batches.size} batches from ${posts.size} posts. " +
                "Avg batch size: ${if (batches.isNotEmpty()) posts.size / batches.size else 0}")

        return batches
    }

    private fun truncatePost(post: PostDto, maxTokens: Int): PostDto {
        val maxChars = maxTokens / 3 // 대략적인 문자 수 추정
        return if (post.content.length > maxChars) {
            post.copy(
                content = post.content.take(maxChars) + "\n\n[내용이 잘렸습니다]"
            )
        } else {
            post
        }
    }
}
