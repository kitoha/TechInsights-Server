package com.techinsights.domain.service.summary

import com.techinsights.domain.dto.summary.ArticleInput
import com.techinsights.domain.dto.summary.SummaryResultWithId
import kotlinx.coroutines.flow.Flow

fun interface GithubReadmeBatchSummarizer {
    fun summarize(items: List<ArticleInput>): Flow<SummaryResultWithId>
}
