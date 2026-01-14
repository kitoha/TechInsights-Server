package com.techinsights.domain.service.gemini

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.techinsights.domain.dto.gemini.ArticleInput
import com.techinsights.domain.dto.gemini.BatchSummaryResponse
import com.techinsights.domain.dto.gemini.SummaryResultWithId
import com.techinsights.domain.exception.JsonTruncationException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class BatchResponseParser {

    private val log = LoggerFactory.getLogger(javaClass)
    private val mapper = jacksonObjectMapper()

    fun parse(responseText: String, articles: List<ArticleInput>): BatchSummaryResponse {
        if (responseText.isBlank()) {
            log.error("Received empty response")
            return createFailureResponse(articles, "Empty response from API")
        }

        return try {
            mapper.readValue(responseText, BatchSummaryResponse::class.java)
        } catch (e: com.fasterxml.jackson.core.JsonParseException) {
            log.error("JSON parsing failed: ${e.message}")
            recoverPartialResponse(responseText, articles)
        } catch (e: com.fasterxml.jackson.databind.JsonMappingException) {
            log.error("JSON mapping failed: ${e.message}")
            recoverPartialResponse(responseText, articles)
        }
    }

    private fun recoverPartialResponse(
        responseText: String,
        articles: List<ArticleInput>
    ): BatchSummaryResponse {
        log.info("Attempting partial response recovery from truncated JSON")

        val recoveredResults = extractCompleteJsonObjects(responseText, articles)

        log.info("Recovered ${recoveredResults.size} of ${articles.size} results")

        // If recovery failed completely or recovered less than 50%, throw exception to trigger Binary Search
        if (recoveredResults.isEmpty() || recoveredResults.size < articles.size * 0.5) {
            val message = "JSON truncation: Recovered ${recoveredResults.size}/${articles.size} results (< 50%). Triggering Binary Search."
            log.warn(message)
            throw JsonTruncationException(message)
        }

        val failedResults = createFailedResults(articles, recoveredResults)

        return BatchSummaryResponse(recoveredResults + failedResults)
    }

    private fun extractCompleteJsonObjects(
        responseText: String,
        articles: List<ArticleInput>
    ): List<SummaryResultWithId> {
        val results = mutableListOf<SummaryResultWithId>()

        for (article in articles) {
            try {
                val jsonObject = findCompleteJsonObject(responseText, article.id) ?: continue
                val result = mapper.readValue(jsonObject, SummaryResultWithId::class.java)
                results.add(result)
            } catch (e: Exception) {
                log.warn("Failed to recover result for article ID ${article.id}: ${e.message}")
            }
        }

        return results
    }

    private fun findCompleteJsonObject(responseText: String, articleId: String): String? {
        val idPattern = """"id":"$articleId""""
        val startIndex = responseText.indexOf(idPattern)

        if (startIndex == -1) return null

        val objectStart = findOpeningBrace(responseText, startIndex) ?: return null
        val objectEnd = findClosingBrace(responseText, objectStart) ?: return null

        return responseText.substring(objectStart, objectEnd)
    }

    private fun findOpeningBrace(text: String, fromIndex: Int): Int? {
        for (i in fromIndex downTo 0) {
            if (text[i] == '{') return i
        }
        return null
    }

    private fun findClosingBrace(text: String, startIndex: Int): Int? {
        var braceCount = 1
        var index = startIndex + 1

        while (index < text.length && braceCount > 0) {
            when (text[index]) {
                '{' -> braceCount++
                '}' -> braceCount--
            }
            index++
        }

        return if (braceCount == 0) index else null
    }

    private fun createFailedResults(
        articles: List<ArticleInput>,
        recoveredResults: List<SummaryResultWithId>
    ): List<SummaryResultWithId> {
        val recoveredIds = recoveredResults.map { it.id }.toSet()

        return articles
            .filter { it.id !in recoveredIds }
            .map {
                SummaryResultWithId(
                    id = it.id,
                    success = false,
                    summary = null,
                    categories = null,
                    preview = null,
                    error = "Response truncated - recovery failed"
                )
            }
    }

    fun createFailureResponse(
        articles: List<ArticleInput>,
        errorMessage: String
    ): BatchSummaryResponse {
        val results = articles.map { article ->
            SummaryResultWithId(
                id = article.id,
                success = false,
                summary = null,
                categories = null,
                preview = null,
                error = errorMessage
            )
        }
        return BatchSummaryResponse(results)
    }
}
