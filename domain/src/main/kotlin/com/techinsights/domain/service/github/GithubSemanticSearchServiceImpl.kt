package com.techinsights.domain.service.github

import com.techinsights.domain.dto.github.GithubSemanticSearchResult
import com.techinsights.domain.repository.github.GithubRepositoryRepository
import com.techinsights.domain.service.embedding.EmbeddingService
import org.springframework.stereotype.Service

@Service
class GithubSemanticSearchServiceImpl(
    private val embeddingService: EmbeddingService,
    private val githubRepositoryRepository: GithubRepositoryRepository,
) : GithubSemanticSearchService {

    override fun search(query: String, size: Int): List<GithubSemanticSearchResult> {
        val cappedSize = minOf(size, MAX_SIZE)
        val vector = embeddingService.generateQuestionEmbedding(query)
        val vectorString = vector.joinToString(",", "[", "]")

        return githubRepositoryRepository
            .findSimilarRepositories(vectorString, cappedSize.toLong())
            .mapIndexed { index, repo ->
                GithubSemanticSearchResult(
                    fullName = repo.fullName,
                    repoName = repo.repoName,
                    description = repo.description,
                    readmeSummary = repo.readmeSummary,
                    primaryLanguage = repo.primaryLanguage,
                    starCount = repo.starCount,
                    ownerName = repo.ownerName,
                    ownerAvatarUrl = repo.ownerAvatarUrl,
                    topics = repo.topics
                        ?.split(",")
                        ?.map { it.trim() }
                        ?.filter { it.isNotBlank() }
                        ?: emptyList(),
                    htmlUrl = repo.htmlUrl,
                    similarityScore = 1.0 / (1.0 + repo.distance),
                    rank = index + 1,
                )
            }
    }

    companion object {
        private const val MAX_SIZE = 20
    }
}
