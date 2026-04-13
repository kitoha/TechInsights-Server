package com.techinsights.domain.service.gemini

import com.techinsights.domain.dto.community.CommunityAnalysisInput
import com.techinsights.domain.dto.community.CommunityAnalysisResult
import kotlinx.coroutines.flow.Flow

fun interface CommunityAnalyzer {
    fun analyze(items: List<CommunityAnalysisInput>): Flow<CommunityAnalysisResult>
}
