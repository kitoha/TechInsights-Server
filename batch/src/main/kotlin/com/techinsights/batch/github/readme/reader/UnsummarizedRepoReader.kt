package com.techinsights.batch.github.readme.reader

import com.techinsights.batch.github.readme.config.props.GithubReadmeBatchProperties
import com.techinsights.domain.dto.github.GithubRepositoryDto
import com.techinsights.domain.repository.github.GithubRepositoryRepository
import org.slf4j.LoggerFactory
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.ItemReader
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
@StepScope
class UnsummarizedRepoReader(
    private val repository: GithubRepositoryRepository,
    private val properties: GithubReadmeBatchProperties,
    @Value("#{jobParameters['maxItemsPerRun']}") private val maxItemsPerRunParam: Long?,
) : ItemReader<GithubRepositoryDto> {

    private val maxItemsPerRun: Int = maxItemsPerRunParam?.toInt() ?: properties.maxItemsPerRun

    private val buffer: ArrayDeque<GithubRepositoryDto> = ArrayDeque()
    private var lastStarCount: Long? = null
    private var lastId: Long? = null
    private var exhausted = false
    private var totalRead = 0

    override fun read(): GithubRepositoryDto? {
        if (totalRead >= maxItemsPerRun) {
            log.info("[UnsummarizedRepoReader] maxItemsPerRun($maxItemsPerRun) reached, stopping")
            return null
        }
        if (buffer.isEmpty() && !exhausted) {
            loadNextPage()
        }
        return buffer.removeFirstOrNull()?.also { totalRead++ }
    }

    private fun loadNextPage() {
        val page = repository.findUnsummarized(properties.pageSize, lastStarCount, lastId)
        if (page.isEmpty()) {
            exhausted = true
            return
        }
        buffer.addAll(page)
        val last = page.last()
        lastStarCount = last.starCount
        lastId = last.id
        log.info("[UnsummarizedRepoReader] Loaded ${page.size} repos (cursor: starCount=$lastStarCount, id=$lastId)")
    }

    companion object {
        private val log = LoggerFactory.getLogger(UnsummarizedRepoReader::class.java)
    }
}
