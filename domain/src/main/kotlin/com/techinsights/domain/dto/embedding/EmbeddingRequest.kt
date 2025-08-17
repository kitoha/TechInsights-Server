package com.techinsights.domain.dto.embedding

data class EmbeddingRequest(
    val content: String,
    val categories: List<String>,
    val companyName: String
) {
    fun toPromptString(): String {
        return """
            카테고리: ${categories.joinToString(", ")}
            회사명: $companyName
            내용: $content
        """.trimIndent()
    }
}
