package com.techinsights.domain.service.gemini

import com.techinsights.domain.dto.gemini.ArticleInput
import com.techinsights.domain.dto.gemini.SummaryResultWithId
import com.techinsights.domain.enums.GeminiModelType
import kotlinx.coroutines.flow.Flow

fun interface BatchArticleSummarizer {
    fun summarizeBatch(
        articles: List<ArticleInput>,
        modelType: GeminiModelType
    ): Flow<SummaryResultWithId>
}
