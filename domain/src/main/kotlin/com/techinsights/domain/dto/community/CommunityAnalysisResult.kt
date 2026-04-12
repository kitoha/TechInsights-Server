package com.techinsights.domain.dto.community

data class CommunityAnalysisResult(
    val id: String,
    val success: Boolean,
    val postClassifications: List<PostClassification> = emptyList(),
    val insights: List<CommunityInsight>? = null,
    val error: String? = null,
) {
    data class PostClassification(
        val url: String,
        val sentiment: String,  // "POSITIVE" | "NEUTRAL" | "NEGATIVE"
    )

    data class CommunityInsight(
        val text: String,
        val tone: String,
    )
}
