package com.techinsights.domain.service.gemini

import com.techinsights.domain.dto.gemini.SummaryResult

interface ArticleSummarizer {
  fun summarize(article: String): SummaryResult
}