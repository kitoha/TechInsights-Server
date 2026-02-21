package com.techinsights.batch.summary.builder

import com.techinsights.batch.summary.config.BatchBuildConfig
import com.techinsights.domain.config.gemini.GeminiProperties
import org.springframework.stereotype.Component

@Component
class BatchLimitChecker(
    private val config: BatchBuildConfig,
    private val geminiProperties: GeminiProperties
) {
    val maxOutputTokensAllowed: Int by lazy {
        (geminiProperties.maxOutputTokens * config.outputSafetyMargin).toInt()
    }

    fun exceedsInputLimit(currentTokens: Int, additionalTokens: Int): Boolean {
        return currentTokens + additionalTokens > config.maxTokensPerRequest
    }

    fun exceedsOutputLimit(batchSize: Int): Boolean {
        val estimatedOutput = estimateOutputTokens(batchSize)
        return estimatedOutput > maxOutputTokensAllowed
    }

    fun exceedsBatchSize(currentSize: Int): Boolean {
        return currentSize >= config.maxBatchSize
    }

    fun exceedsMaxTokens(tokens: Int): Boolean {
        return tokens > config.maxTokensPerRequest
    }

    fun estimateOutputTokens(batchSize: Int): Int {
        return (batchSize * config.avgTokensPerSummary) + config.jsonOverheadTokens
    }
}
