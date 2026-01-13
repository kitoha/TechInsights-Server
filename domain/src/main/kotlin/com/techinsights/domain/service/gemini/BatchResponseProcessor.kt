package com.techinsights.domain.service.gemini

import com.techinsights.domain.dto.gemini.ArticleInput
import com.techinsights.domain.dto.gemini.BatchSummaryResponse
import com.techinsights.domain.dto.gemini.SummaryResultWithId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class BatchResponseProcessor(
    private val validator: BatchSummaryValidator
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun process(
        response: BatchSummaryResponse,
        articles: List<ArticleInput>,
        categories: Set<String>
    ): BatchSummaryResponse {
        validateResponseCount(response, articles)

        val validatedResults = validateResults(response.results, articles, categories)
        val missingResults = createMissingResults(validatedResults, articles)

        return BatchSummaryResponse(validatedResults + missingResults)
    }

    private fun validateResponseCount(response: BatchSummaryResponse, articles: List<ArticleInput>) {
        if (response.results.size != articles.size) {
            log.warn("Response count mismatch: expected ${articles.size}, got ${response.results.size}")
        }
    }

    private fun validateResults(
        results: List<SummaryResultWithId>,
        articles: List<ArticleInput>,
        categories: Set<String>
    ): List<SummaryResultWithId> {
        return results.map { result ->
            val input = articles.find { it.id == result.id }

            when {
                input == null -> result.copy(success = false, error = "Unknown ID")
                else -> validateResult(input, result, categories)
            }
        }
    }

    private fun validateResult(
        input: ArticleInput,
        result: SummaryResultWithId,
        categories: Set<String>
    ): SummaryResultWithId {
        val validation = validator.validate(input, result, categories)

        return if (validation.isValid) {
            result
        } else {
            result.copy(
                success = false,
                error = validation.errors.joinToString(", ")
            )
        }
    }

    private fun createMissingResults(
        validatedResults: List<SummaryResultWithId>,
        articles: List<ArticleInput>
    ): List<SummaryResultWithId> {
        val receivedIds = validatedResults.map { it.id }.toSet()

        return articles
            .filter { it.id !in receivedIds }
            .map {
                SummaryResultWithId(
                    id = it.id,
                    success = false,
                    summary = null,
                    categories = null,
                    preview = null,
                    error = "No response received"
                )
            }
    }
}
