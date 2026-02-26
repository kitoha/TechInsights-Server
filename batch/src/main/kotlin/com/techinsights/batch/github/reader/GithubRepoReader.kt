package com.techinsights.batch.github.reader

import com.techinsights.batch.github.config.props.GithubApiProperties
import com.techinsights.batch.github.config.props.GithubBatchProperties
import com.techinsights.batch.github.dto.GithubSearchResponse
import com.techinsights.batch.github.service.GithubTrendingFetchService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import org.slf4j.LoggerFactory
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.ItemReader
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
@StepScope
class GithubRepoReader(
    private val fetchService: GithubTrendingFetchService,
    private val batchProperties: GithubBatchProperties,
    private val apiProperties: GithubApiProperties,
    @Qualifier("ioDispatcher") private val dispatcher: CoroutineDispatcher,
) : ItemReader<GithubSearchResponse.Item> {

    private val log = LoggerFactory.getLogger(javaClass)
    private var buffer: Iterator<GithubSearchResponse.Item>? = null

    override fun read(): GithubSearchResponse.Item? {
        if (buffer == null) {
            buffer = runBlocking { fetchAll() }.iterator()
        }
        return if (buffer!!.hasNext()) buffer!!.next() else null
    }

    private suspend fun fetchAll(): List<GithubSearchResponse.Item> {
        val seen = ConcurrentHashMap.newKeySet<String>()

        return supervisorScope {
            batchProperties.queries.map { queryConfig ->
                async(dispatcher) {
                    try {
                        fetchAllPagesForQuery(queryConfig.query, seen)
                    } catch (e: Exception) {
                        log.error("Query '${queryConfig.query}' 전체 실패 — 건너뜀: ${e.message}", e)
                        emptyList()
                    }
                }
            }.awaitAll().flatten()
        }
    }

    private suspend fun fetchAllPagesForQuery(
        query: String,
        seen: MutableSet<String>,
    ): List<GithubSearchResponse.Item> {
        val firstPage = fetchService.fetchPage(query, 1)
        val totalPages = minOf(
            (firstPage.totalCount + apiProperties.perPage - 1) / apiProperties.perPage,
            MAX_PAGES,
        )
        log.info("Query '$query': totalCount=${firstPage.totalCount}, totalPages=$totalPages")

        val items = firstPage.items.filter { seen.add(it.fullName) }.toMutableList()

        for (page in 2..totalPages) {
            try {
                val response = fetchService.fetchPage(query, page)
                items.addAll(response.items.filter { seen.add(it.fullName) })
            } catch (e: Exception) {
                log.warn("Query '$query' page $page 실패 — 건너뜀: ${e.message}")
            }
        }

        return items
    }

    companion object {
        /** GitHub Search API 최대 페이지 수 (100건 × 10페이지 = 1000건) */
        private const val MAX_PAGES = 10
    }
}
