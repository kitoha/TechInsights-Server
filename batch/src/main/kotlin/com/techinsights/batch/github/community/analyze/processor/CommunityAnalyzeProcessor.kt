package com.techinsights.batch.github.community.analyze.processor

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.techinsights.domain.dto.community.CommunityAnalysisInput
import com.techinsights.domain.dto.community.CommunityPost
import com.techinsights.domain.dto.github.GithubRepositoryDto
import org.slf4j.LoggerFactory
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component

@Component
class CommunityAnalyzeProcessor(
    private val objectMapper: ObjectMapper,
) : ItemProcessor<GithubRepositoryDto, CommunityAnalysisInput> {

    override fun process(item: GithubRepositoryDto): CommunityAnalysisInput? {
        val highlights = item.communityHighlights ?: run {
            log.warn("[CommunityAnalyzeProcessor] ${item.fullName} has no communityHighlights, skipping")
            return null
        }

        return try {
            val map = objectMapper.readValue<Map<String, List<CommunityPost>>>(highlights)
            CommunityAnalysisInput(
                repoFullName = item.fullName,
                repoName = item.repoName,
                hnPosts = map["hnPosts"] ?: emptyList(),
                redditPosts = map["redditPosts"] ?: emptyList(),
            )
        } catch (e: Exception) {
            log.warn("[CommunityAnalyzeProcessor] Failed to parse highlights for ${item.fullName}: ${e.message}")
            null
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(CommunityAnalyzeProcessor::class.java)
    }
}
