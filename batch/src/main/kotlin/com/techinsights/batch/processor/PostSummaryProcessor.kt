package com.techinsights.batch.processor

import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.enums.Category
import com.techinsights.domain.enums.GeminiModelType
import com.techinsights.domain.service.gemini.ArticleSummarizer
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.slf4j.LoggerFactory
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component

@Component
class PostSummaryProcessor(
    private val summarizer: ArticleSummarizer
) : ItemProcessor<List<PostDto>, List<PostDto>> {

    private val log = LoggerFactory.getLogger(PostSummaryProcessor::class.java)
    private val semaphore = Semaphore(10)

    override fun process(items: List<PostDto>): List<PostDto>? {
        return runBlocking {
            processSuspending(items)
        }
    }

    private suspend fun processSuspending(items: List<PostDto>): List<PostDto> {
        return coroutineScope {
            val deferredSummaries = items.map { item ->
                async {
                    semaphore.withPermit {
                        try {
                            val summarized =
                                summarizer.summarize(item.content, GeminiModelType.GEMINI_2_5_FLASH)
                            val content = summarized.summary
                            val categories =
                                summarized.categories.map { Category.valueOf(it) }.toSet()
                            val preview = summarized.preview
                            val summarizedItem = item.copy(
                                content = content,
                                preview = preview,
                                categories = categories,
                                isSummary = true
                            )
                            log.info("Successfully summarized item with id: ${item.id}")
                            summarizedItem
                        } catch (e: Exception) {
                            log.warn(
                                "Failed to summarize item with id: ${item.id}, keeping original item",
                                e
                            )
                            item
                        }
                    }
                }
            }
            deferredSummaries.awaitAll()
        }
    }
}