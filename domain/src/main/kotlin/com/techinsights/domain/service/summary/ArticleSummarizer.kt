package com.techinsights.domain.service.summary

import com.techinsights.domain.dto.summary.SummaryResult
import com.techinsights.domain.enums.GeminiModelType

interface ArticleSummarizer {
  fun summarize(article: String, modelType: GeminiModelType): SummaryResult
}