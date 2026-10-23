package com.techinsights.domain.service.summary

import com.techinsights.domain.dto.summary.ArticleInput
import com.techinsights.domain.dto.summary.SummaryResultWithId
import com.techinsights.domain.enums.GeminiModelType
import kotlinx.coroutines.flow.Flow

fun interface BatchArticleSummarizer {
    fun summarizeBatch(
        articles: List<ArticleInput>,
        modelType: GeminiModelType
    ): Flow<SummaryResultWithId>
}
