package com.techinsights.domain.service.gemini

import com.techinsights.domain.dto.gemini.SummaryResult
import com.techinsights.domain.enums.GeminiModelType

interface ArticleSummarizer {
  fun summarize(article: String, modelType: GeminiModelType): SummaryResult
}