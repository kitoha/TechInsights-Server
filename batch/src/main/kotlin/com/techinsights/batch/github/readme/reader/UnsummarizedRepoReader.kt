package com.techinsights.batch.github.readme.reader

import com.techinsights.domain.dto.github.GithubRepositoryDto
import com.techinsights.domain.repository.github.GithubRepositoryRepository
import org.slf4j.LoggerFactory
import org.springframework.batch.item.ItemReader
import org.springframework.stereotype.Component

@Component
class UnsummarizedRepoReader(
    private val repository: GithubRepositoryRepository,
    private val pageSize: Int = DEFAULT_PAGE_SIZE,
) : ItemReader<GithubRepositoryDto> {

    private val buffer: ArrayDeque<GithubRepositoryDto> = ArrayDeque()
    private var lastStarCount: Long? = null
    private var lastId: Long? = null
    private var exhausted = false

    override fun read(): GithubRepositoryDto? {
        if (buffer.isEmpty() && !exhausted) {
            loadNextPage()
        }
        return buffer.removeFirstOrNull()
    }

    private fun loadNextPage() {
        val page = repository.findUnsummarized(pageSize, lastStarCount, lastId)
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
        private const val DEFAULT_PAGE_SIZE = 100
    }
}
