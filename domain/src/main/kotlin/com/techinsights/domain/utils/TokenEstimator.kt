package com.techinsights.domain.utils

object TokenEstimator {

    fun estimateTokens(text: String): Int {
        val koreanChars = text.count { it.code >= 0xAC00 && it.code <= 0xD7AF }
        val otherChars = text.length - koreanChars

        return (koreanChars * 3) + (otherChars / 2)
    }

    fun estimateInputTokens(content: String): Int {
        val basePromptTokens = 500  // 고정 프롬프트 부분
        val contentTokens = estimateTokens(content)
        return basePromptTokens + contentTokens
    }

    fun estimateOutputTokens(): Int {
        return 1000
    }

    fun estimateTotalTokens(content: String): Int {
        return estimateInputTokens(content) + estimateOutputTokens()
    }
}
