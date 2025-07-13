package com.techinsights.batch.processor

import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.enums.Category
import com.techinsights.domain.service.gemini.ArticleSummarizer
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component

@Component
class PostSummaryProcessor(
    private val summarizer: ArticleSummarizer
) : ItemProcessor<List<PostDto>, List<PostDto>> {

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
                        val summarized = summarizer.summarize(item.content)
                        val content = summarized.summary
                        val categories = summarized.categories.map { Category.valueOf(it) }.toSet()
                        item.copy(content = content, categories = categories, isSummary = true)
                    }
                }
            }
            deferredSummaries.awaitAll()
        }
    }
}