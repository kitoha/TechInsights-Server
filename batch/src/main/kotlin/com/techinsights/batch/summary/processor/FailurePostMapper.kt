package com.techinsights.batch.summary.processor

import com.techinsights.batch.summary.dto.BatchFailure
import com.techinsights.batch.summary.mapper.ErrorTypeMapper.toSummaryErrorType
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.enums.ErrorType
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
        // 일일 쿼터 소진은 게시글 실패가 아니다: 결과에서 빼서 summary_failure_count를 올리지 않고 다음 실행에서 다시 처리한다.
        val quotaExhaustedIds = failures.filter { it.errorType == ErrorType.QUOTA_EXHAUSTED }.map { it.post.id }.toSet()

        return originalPosts
            .filter { it.id !in successIds && it.id !in quotaExhaustedIds }
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
