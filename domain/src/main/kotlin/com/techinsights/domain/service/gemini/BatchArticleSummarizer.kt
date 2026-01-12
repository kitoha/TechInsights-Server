package com.techinsights.domain.service.gemini

import com.techinsights.domain.dto.gemini.ArticleInput
import com.techinsights.domain.dto.gemini.BatchSummaryResponse
import com.techinsights.domain.enums.GeminiModelType

fun interface BatchArticleSummarizer {
    suspend fun summarizeBatch(
        articles: List<ArticleInput>,
        modelType: GeminiModelType
    ): BatchSummaryResponse
}
