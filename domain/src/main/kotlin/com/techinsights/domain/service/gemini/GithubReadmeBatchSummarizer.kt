package com.techinsights.domain.service.gemini

import com.techinsights.domain.dto.gemini.ArticleInput
import com.techinsights.domain.dto.gemini.SummaryResultWithId
import kotlinx.coroutines.flow.Flow

fun interface GithubReadmeBatchSummarizer {
    fun summarize(items: List<ArticleInput>): Flow<SummaryResultWithId>
}
