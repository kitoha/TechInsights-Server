package com.techinsights.batch.processor

import com.techinsights.batch.dto.BatchFailure
import com.techinsights.batch.mapper.ErrorTypeMapper.toSummaryErrorType
import com.techinsights.domain.dto.post.PostDto
import org.springframework.stereotype.Component

@Component
class FailurePostMapper {

    fun mapFailuresToPosts(
        originalPosts: List<PostDto>,
        successes: List<PostDto>,
        failures: List<BatchFailure>
    ): List<PostDto> {
        val failureMap = failures.associateBy { it.post.id }
        val successIds = successes.map { it.id }.toSet()

        return originalPosts
            .filter { it.id !in successIds }
            .map { post -> applyFailureMetadata(post, failureMap[post.id]) }
    }

    private fun applyFailureMetadata(post: PostDto, failure: BatchFailure?): PostDto {
        return if (failure != null) {
            post.copy().apply {
                failureErrorType = failure.errorType.toSummaryErrorType()
                failureErrorMessage = failure.reason
                failureBatchSize = failure.batchSize
                failureIsBatchFailure = failure.isBatchFailure
            }
        } else {
            post
        }
    }
}
