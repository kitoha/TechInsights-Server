package com.techinsights.batch.summary.service

import com.techinsights.domain.dto.gemini.SummaryResultWithId
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.enums.Category
import org.springframework.stereotype.Component

@Component
class PostDtoConverter {
    
    fun convert(post: PostDto, result: SummaryResultWithId): PostDto {
        return post.copy(
            content = result.summary ?: post.content,
            preview = result.preview,
            categories = parseCategories(result.categories),
            isSummary = true
        )
    }
    
    private fun parseCategories(categories: List<String>?): Set<Category> {
        return categories
            ?.mapNotNull { runCatching { Category.valueOf(it) }.getOrNull() }
            ?.toSet()
            ?: emptySet()
    }
}
