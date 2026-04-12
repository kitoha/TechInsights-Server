package com.techinsights.batch.github.community.analyze.processor

import com.techinsights.domain.dto.community.CommunityAnalysisInput
import com.techinsights.domain.dto.github.GithubRepositoryDto
import org.slf4j.LoggerFactory
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component

@Component
class CommunityAnalyzeProcessor : ItemProcessor<GithubRepositoryDto, CommunityAnalysisInput> {

    override fun process(item: GithubRepositoryDto): CommunityAnalysisInput? {
        log.debug("[CommunityAnalyzeProcessor] Skipping ${item.fullName} — pending Step 7 rewrite")
        return null
    }

    companion object {
        private val log = LoggerFactory.getLogger(CommunityAnalyzeProcessor::class.java)
    }
}
