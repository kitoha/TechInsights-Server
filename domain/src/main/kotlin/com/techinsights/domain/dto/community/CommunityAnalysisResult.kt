package com.techinsights.domain.dto.community

data class CommunityAnalysisResult(
    val id: String,
    val success: Boolean,
    val sentiment: SentimentBreakdown? = null,
    val insights: List<CommunityInsight>? = null,
    val error: String? = null,
) {
    data class SentimentBreakdown(
        val positive: Int,
        val neutral: Int,
        val negative: Int,
    )

    data class CommunityInsight(
        val text: String,
        val tone: String,
    )
}
