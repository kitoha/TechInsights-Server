package com.techinsights.domain.repository.post

/**
 * Native query projection for findSimilarPosts.
 * Carries the pgvector cosine distance (<->) alongside PostEmbedding fields.
 * embeddingVector is intentionally omitted — the semantic search pipeline
 * does not need the raw vector after retrieving similar posts.
 */
interface PostEmbeddingWithDistance {
    val postId: Long
    val companyName: String
    val categories: String
    val content: String
    val distance: Double
}
