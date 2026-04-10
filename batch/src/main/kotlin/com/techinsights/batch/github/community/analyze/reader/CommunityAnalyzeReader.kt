package com.techinsights.batch.github.community.analyze.reader

import com.techinsights.batch.github.community.analyze.config.props.CommunityAnalyzeBatchProperties
import com.techinsights.domain.dto.github.GithubRepositoryDto
import com.techinsights.domain.repository.github.GithubRepositoryRepository
import org.slf4j.LoggerFactory
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.ItemReader
import org.springframework.stereotype.Component

@Component
@StepScope
class CommunityAnalyzeReader(
    private val repository: GithubRepositoryRepository,
    private val properties: CommunityAnalyzeBatchProperties,
) : ItemReader<GithubRepositoryDto> {

    private val buffer: ArrayDeque<GithubRepositoryDto> = ArrayDeque()
    private var lastId: Long? = null
    private var exhausted = false
    private var totalRead = 0

    override fun read(): GithubRepositoryDto? {
        if (totalRead >= properties.maxItemsPerRun) {
            log.info("[CommunityAnalyzeReader] maxItemsPerRun(${properties.maxItemsPerRun}) reached, stopping")
            return null
        }
        if (buffer.isEmpty() && !exhausted) {
            loadNextPage()
        }
        return buffer.removeFirstOrNull()?.also { totalRead++ }
    }

    private fun loadNextPage() {
        val page = repository.findForCommunityAnalyze(
            pageSize = properties.pageSize,
            afterId = lastId,
        )
        if (page.isEmpty()) {
            exhausted = true
            return
        }
        buffer.addAll(page)
        lastId = page.last().id
        log.info("[CommunityAnalyzeReader] Loaded ${page.size} repos (cursor: id=$lastId)")
    }

    companion object {
        private val log = LoggerFactory.getLogger(CommunityAnalyzeReader::class.java)
    }
}
