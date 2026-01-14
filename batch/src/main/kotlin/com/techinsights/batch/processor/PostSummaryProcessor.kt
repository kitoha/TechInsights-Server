package com.techinsights.batch.processor

import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.enums.Category
import com.techinsights.domain.enums.GeminiModelType
import com.techinsights.domain.service.gemini.ArticleSummarizer
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component

@Component
class PostSummaryProcessor(
    private val summarizer: ArticleSummarizer
) : ItemProcessor<PostDto, PostDto> {

    private val log = LoggerFactory.getLogger(PostSummaryProcessor::class.java)

    override fun process(item: PostDto): PostDto {
        return runBlocking {
            processSuspending(item)
        }
    }

    private suspend fun processSuspending(item: PostDto): PostDto {
        try {
            val summarized =
                summarizer.summarize(item.content, GeminiModelType.GEMINI_3_FLASH)
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
            return summarizedItem
        } catch (e: Exception) {
            log.warn("Failed to summarize item with id: ${item.id}, error: ${e.message}")
            return item
        }
    }
}