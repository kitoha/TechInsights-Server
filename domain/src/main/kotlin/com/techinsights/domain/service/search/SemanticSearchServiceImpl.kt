package com.techinsights.domain.service.search

import com.techinsights.domain.config.search.SemanticSearchProperties
import com.techinsights.domain.dto.search.SemanticSearchResult
import com.techinsights.domain.repository.post.PostEmbeddingRepository
import com.techinsights.domain.repository.post.PostRepository
import com.techinsights.domain.service.embedding.EmbeddingService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.techinsights.domain.utils.Tsid

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
        val fetchLimit = if (companyId != null) resolvedSize * OVER_FETCH_MULTIPLIER else resolvedSize

        val questionVector = embeddingService.generateQuestionEmbedding(query)
        val vectorString = formatVectorString(questionVector)

        val embeddings = postEmbeddingRepository.findSimilarPostsAll(
            targetVector = vectorString,
            limit = fetchLimit.toLong()
        )

        if (embeddings.isEmpty()) return emptyList()

        val postIds = embeddings.map { it.postId }
        val postsById = postRepository.findAllByIdIn(postIds).associateBy { it.id }

        val orderedResults = embeddings
            .mapNotNull { embedding ->
                val post = postsById[embedding.postId] ?: return@mapNotNull null
                embedding to post
            }
            .filter { (_, post) -> matchesCompany(post, companyId) }
            .take(resolvedSize)

        return orderedResults.mapIndexed { index, (embedding, post) ->
            SemanticSearchResult(
                post = post,
                similarityScore = toSimilarityScore(embedding.distance ?: 0.0),
                rank = index + 1
            )
        }
    }

    private fun resolveSize(requestedSize: Int): Int =
        requestedSize.coerceIn(1, properties.maxSize)

    private fun formatVectorString(vector: List<Float>): String =
        vector.joinToString(prefix = "[", postfix = "]")

    private fun matchesCompany(post: com.techinsights.domain.dto.post.PostDto, companyId: Long?): Boolean {
        if (companyId == null) return true
        return post.company.id == Tsid.encode(companyId)
    }

    /** Cosine distance is in [0, 2], so the score is in [1/3, 1]; 1.0 means identical direction. */
    private fun toSimilarityScore(distance: Double): Double =
        1.0 / (1.0 + distance)

    companion object {
        private const val OVER_FETCH_MULTIPLIER = 3
    }
}
