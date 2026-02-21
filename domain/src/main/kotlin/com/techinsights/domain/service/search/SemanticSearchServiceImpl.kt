package com.techinsights.domain.service.search

import com.techinsights.domain.config.search.SemanticSearchProperties
import com.techinsights.domain.dto.search.SemanticSearchResult
import com.techinsights.domain.repository.post.PostEmbeddingRepository
import com.techinsights.domain.repository.post.PostRepository
import com.techinsights.domain.service.embedding.EmbeddingService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class SemanticSearchServiceImpl(
    private val embeddingService: EmbeddingService,
    private val postEmbeddingRepository: PostEmbeddingRepository,
    private val postRepository: PostRepository,
    private val properties: SemanticSearchProperties
) : SemanticSearchService {

    override fun search(query: String, size: Int, companyId: Long?): List<SemanticSearchResult> {
        val resolvedSize = resolveSize(size)

        val questionVector = embeddingService.generateQuestionEmbedding(query)
        val vectorString = formatVectorString(questionVector)

        val embeddings = postEmbeddingRepository.findSimilarPosts(
            targetVector = vectorString,
            excludeIds = emptyList(),
            limit = resolvedSize.toLong()
        )

        if (embeddings.isEmpty()) return emptyList()

        val postIds = embeddings.map { it.postId }
        val posts = postRepository.findAllByIdIn(postIds)

        val filteredPosts = filterByCompany(posts, companyId)

        return filteredPosts.mapIndexed { index, post ->
            val distance = calculateDistanceByRank(index)
            SemanticSearchResult(
                post = post,
                similarityScore = toSimilarityScore(distance),
                rank = index + 1
            )
        }
    }

    private fun resolveSize(requestedSize: Int): Int =
        requestedSize.coerceIn(1, properties.maxSize)

    private fun formatVectorString(vector: List<Float>): String =
        vector.joinToString(prefix = "[", postfix = "]")

    private fun filterByCompany(posts: List<com.techinsights.domain.dto.post.PostDto>, companyId: Long?): List<com.techinsights.domain.dto.post.PostDto> {
        if (companyId == null) return posts
        return posts.filter { it.company.id == companyId.toString() }
    }

    private fun toSimilarityScore(distance: Double): Double =
        1.0 / (1.0 + distance)

    private fun calculateDistanceByRank(rank: Int): Double =
        rank * DISTANCE_STEP_PER_RANK

    companion object {
        private const val DISTANCE_STEP_PER_RANK = 0.1
    }
}
