package com.techinsights.domain.service.summary

import com.techinsights.domain.dto.summary.SummaryResult

interface ArticleSummarizer {
  fun summarize(article: String): SummaryResult
}