package com.techinsights.batch.processor

import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.enums.Category
import com.techinsights.domain.service.gemini.ArticleSummarizer
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component

@Component
class PostSummaryProcessor(
  private val summarizer: ArticleSummarizer
) : ItemProcessor<PostDto, PostDto> {

  override fun process(item: PostDto): PostDto {
    val summarized = summarizer.summarize(item.content)
    val content = summarized.summary
    val categories = summarized.categories.map { Category.valueOf(it) }.toSet()
    return item.copy(content = content, categories = categories, isSummary = true)
  }
}