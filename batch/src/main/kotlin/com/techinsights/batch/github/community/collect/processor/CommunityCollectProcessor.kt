package com.techinsights.batch.github.community.collect.processor

import com.techinsights.batch.github.community.dto.CommunityBuzzInput
import com.techinsights.batch.github.community.service.CommunityFetcher
import com.techinsights.domain.dto.github.GithubRepositoryDto
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component

@Component
class CommunityCollectProcessor(
    private val fetcher: CommunityFetcher,
) : ItemProcessor<GithubRepositoryDto, CommunityBuzzInput> {

    override fun process(item: GithubRepositoryDto): CommunityBuzzInput {
        log.debug("[CommunityCollectProcessor] Fetching community data for ${item.fullName}")
        return runBlocking {
            fetcher.fetch(
                repoFullName = item.fullName,
                ownerName = item.ownerName,
                repoName = item.repoName,
                prevMentionCount = null,
                updateCount = 0,
            )
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(CommunityCollectProcessor::class.java)
    }
}
