package com.techinsights.batch.summary.writer

import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.entity.post.PostSummaryFailure
import com.techinsights.domain.enums.SummaryErrorType
import com.techinsights.domain.repository.post.PostRepository
import com.techinsights.domain.repository.post.PostSummaryFailureRepository
import com.techinsights.domain.service.company.CompanyViewCountUpdater
import com.techinsights.domain.utils.Tsid
import org.slf4j.LoggerFactory
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class FlatteningPostWriter(
    private val postRepository: PostRepository,
    private val failureRepository: PostSummaryFailureRepository,
    private val companyViewCountUpdater: CompanyViewCountUpdater
) : ItemWriter<List<PostDto>> {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun write(chunks: Chunk<out List<PostDto>>) {
        val allPosts = chunks.items.flatten()

        if (allPosts.isEmpty()) {
            log.info("No posts to write")
            return
        }

        val summarizedPosts = allPosts.filter { it.isSummary }
        val failedPosts = allPosts.filter { !it.isSummary }

        if (summarizedPosts.isNotEmpty()) {
            val savedPosts = postRepository.saveAll(summarizedPosts)

            val companyIds = savedPosts.map { it.company.id }.toSet()
            companyIds.forEach { companyId ->
                companyViewCountUpdater.incrementPostCount(companyId, 1)
            }

            log.info("Successfully saved ${summarizedPosts.size} summarized posts")
        }

        if (failedPosts.isNotEmpty()) {
            recordFailures(failedPosts)
            log.warn("Recorded ${failedPosts.size} failed posts for retry")
        }

        if (summarizedPosts.isNotEmpty() && failedPosts.isNotEmpty()) {
            log.info("Batch result: ${summarizedPosts.size} succeeded, ${failedPosts.size} failed")
        }
    }

    private fun recordFailures(failedPosts: List<PostDto>) {
        failedPosts.forEach { post ->
            try {
                postRepository.incrementSummaryFailureCount(post.id)

                val failureRecord = PostSummaryFailure(
                    postId = Tsid.decode(post.id),
                    errorType = post.failureErrorType ?: SummaryErrorType.UNKNOWN,
                    errorMessage = post.failureErrorMessage ?: "Summary generation failed - see batch logs for details",
                    batchSize = post.failureBatchSize ?: 1,
                    isBatchFailure = post.failureIsBatchFailure ?: false
                )
                failureRepository.save(failureRecord)

            } catch (e: Exception) {
                log.error("Failed to record failure for post ${post.id}: ${e.message}", e)
            }
        }
    }
}

