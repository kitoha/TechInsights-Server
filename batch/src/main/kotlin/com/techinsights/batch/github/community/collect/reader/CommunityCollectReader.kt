package com.techinsights.batch.github.community.collect.reader

import com.techinsights.batch.github.community.collect.config.props.CommunityCollectBatchProperties
import com.techinsights.domain.dto.github.GithubRepositoryDto
import com.techinsights.domain.repository.github.GithubRepositoryRepository
import org.slf4j.LoggerFactory
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.ItemReader
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
@StepScope
class CommunityCollectReader(
    private val repository: GithubRepositoryRepository,
    private val properties: CommunityCollectBatchProperties,
    @Value("#{jobParameters['maxItemsPerRun']}") private val maxItemsPerRunParam: Long?,
) : ItemReader<GithubRepositoryDto> {

    private val maxItemsPerRun: Int = maxItemsPerRunParam?.toInt() ?: properties.maxItemsPerRun

    private val buffer: ArrayDeque<GithubRepositoryDto> = ArrayDeque()
    private var lastCollectedAt: LocalDateTime? = null
    private var lastId: Long? = null
    private var exhausted = false
    private var totalRead = 0

    override fun read(): GithubRepositoryDto? {
        if (totalRead >= maxItemsPerRun) {
            log.info("[CommunityCollectReader] maxItemsPerRun($maxItemsPerRun) reached, stopping")
            return null
        }
        if (buffer.isEmpty() && !exhausted) {
            loadNextPage()
        }
        return buffer.removeFirstOrNull()?.also { totalRead++ }
    }

    private fun loadNextPage() {
        val now = LocalDateTime.now()
        val noMentionsRefreshAfter = now.minusDays(properties.noMentionsRefreshDays)
        val normalRefreshAfter = now.minusDays(properties.normalRefreshDays)

        val page = repository.findForCommunityCollect(
            pageSize = properties.pageSize,
            afterCollectedAt = lastCollectedAt,
            afterId = lastId,
            noMentionsRefreshAfter = noMentionsRefreshAfter,
            normalRefreshAfter = normalRefreshAfter,
        )

        if (page.isEmpty()) {
            exhausted = true
            return
        }

        buffer.addAll(page)
        val last = page.last()
        lastCollectedAt = last.communityCollectedAt
        lastId = last.id
        log.info("[CommunityCollectReader] Loaded ${page.size} repos (cursor: collectedAt=$lastCollectedAt, id=$lastId)")
    }

    companion object {
        private val log = LoggerFactory.getLogger(CommunityCollectReader::class.java)
    }
}
