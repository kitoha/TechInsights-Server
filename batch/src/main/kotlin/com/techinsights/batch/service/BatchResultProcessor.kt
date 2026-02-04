package com.techinsights.batch.service

import com.techinsights.batch.dto.BatchFailure
import com.techinsights.batch.dto.BatchRequest
import com.techinsights.batch.dto.BatchResult
import com.techinsights.domain.dto.gemini.BatchSummaryResponse
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.enums.ErrorType
import org.springframework.stereotype.Component

@Component
class BatchResultProcessor(
    private val resultValidator: SummaryResultValidator,
    private val postConverter: PostDtoConverter,
    private val resultAssembler: BatchResultAssembler,
    private val retryPolicy: BatchRetryPolicy
) {

    fun processBatchResponse(
        request: BatchRequest,
        batchResponse: BatchSummaryResponse,
        startTime: Long
    ): BatchResult {
        val successes = mutableListOf<PostDto>()
        val failures = mutableListOf<BatchFailure>()

        request.posts.forEach { post ->
            val result = batchResponse.results.find { it.id == post.id }

            when {
                result == null -> failures.add(
                    BatchFailure(post, "No result returned", false, ErrorType.VALIDATION_ERROR)
                )

                !result.success -> failures.add(
                    BatchFailure(
                        post = post,
                        reason = result.error ?: "Unknown error",
                        retryable = result.errorType?.let { retryPolicy.isRetryableError(it) } ?: true,
                        errorType = result.errorType ?: ErrorType.VALIDATION_ERROR
                    )
                )

                else -> {
                    val validation = resultValidator.validate(post.id, post.title, post.content, result)

                    if (validation.isValid) {
                        successes.add(postConverter.convert(post, result))
                    } else {
                        failures.add(
                            BatchFailure(post, validation.errors.joinToString(", "), true, ErrorType.VALIDATION_ERROR)
                        )
                    }
                }
            }
        }

        val duration = System.currentTimeMillis() - startTime
        return resultAssembler.assembleResult(request, successes, failures, duration)
    }

    fun createFailureResult(
        request: BatchRequest,
        reason: String,
        errorType: ErrorType
    ): BatchResult {
        return resultAssembler.assembleFailureResult(
            request,
            reason,
            errorType,
            retryPolicy.isRetryableError(errorType)
        )
    }
}
