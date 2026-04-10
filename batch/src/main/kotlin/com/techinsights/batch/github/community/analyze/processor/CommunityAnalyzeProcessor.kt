package com.techinsights.batch.github.community.analyze.processor

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.techinsights.domain.dto.community.CommunityAnalysisInput
import com.techinsights.domain.dto.community.CommunityPost
import com.techinsights.domain.dto.github.GithubRepositoryDto
import org.slf4j.LoggerFactory
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component

@Component
class CommunityAnalyzeProcessor : ItemProcessor<GithubRepositoryDto, CommunityAnalysisInput> {

    private val objectMapper = jacksonObjectMapper()

    override fun process(item: GithubRepositoryDto): CommunityAnalysisInput? {
        val highlights = item.communityHighlights ?: run {
            log.debug("[CommunityAnalyzeProcessor] ${item.fullName} has no communityHighlights, skipping")
            return null
        }

        val data = try {
            parseHighlights(highlights)
        } catch (e: Exception) {
            log.warn("[CommunityAnalyzeProcessor] Failed to parse communityHighlights for ${item.fullName}: ${e.message}")
            return null
        }

        return CommunityAnalysisInput(
            repoFullName = item.fullName,
            repoName = item.repoName,
            hnPosts = data.hnPosts,
            redditPosts = data.redditPosts,
        )
    }

    private fun parseHighlights(json: String): HighlightsData {
        val tree = objectMapper.readTree(json)
        return HighlightsData(
            hnPosts = parsePosts(tree, "hnPosts"),
            redditPosts = parsePosts(tree, "redditPosts"),
        )
    }

    private fun parsePosts(tree: JsonNode, path: String): List<CommunityPost> {
        val node = tree.path(path)
        return if (node.isMissingNode || node.isNull) emptyList()
        else node.map { postNode ->
            CommunityPost(
                platform = postNode.path("platform").asText(),
                title = postNode.path("title").asText(),
                score = postNode.path("score").asInt(),
                commentCount = postNode.path("commentCount").asInt(),
                username = postNode.path("username").takeUnless { it.isNull }?.asText(),
                url = postNode.path("url").asText(),
            )
        }
    }

    private data class HighlightsData(
        val hnPosts: List<CommunityPost> = emptyList(),
        val redditPosts: List<CommunityPost> = emptyList(),
    )

    companion object {
        private val log = LoggerFactory.getLogger(CommunityAnalyzeProcessor::class.java)
    }
}
