package com.techinsights.batch.writer

import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.repository.post.PostRepository
import com.techinsights.domain.service.company.CompanyViewCountUpdater
import org.slf4j.LoggerFactory
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import org.springframework.stereotype.Component

@Component
class FlatteningPostWriter(
    private val postRepository: PostRepository,
    private val companyViewCountUpdater: CompanyViewCountUpdater
) : ItemWriter<List<PostDto>> {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun write(chunks: Chunk<out List<PostDto>>) {
        val allPosts = chunks.items.flatten()

        if (allPosts.isEmpty()) {
            log.debug("No posts to write")
            return
        }

        val summarizedPosts = allPosts.filter { it.isSummary }

        if (summarizedPosts.isEmpty()) {
            log.warn("No summarized posts in chunk")
            return
        }

        val savedPosts = postRepository.saveAll(summarizedPosts)

        val companyIds = savedPosts.map { it.company.id }.toSet()
        companyIds.forEach { companyId ->
            companyViewCountUpdater.incrementPostCount(companyId, 1)
        }

        log.info("Successfully saved ${savedPosts.size} summarized posts out of ${allPosts.size} total posts")

        val failedCount = allPosts.size - summarizedPosts.size
        if (failedCount > 0) {
            log.warn("$failedCount posts were not summarized and will not be saved")
        }
    }
}
