package com.techinsights.batch.summary.service

import com.techinsights.domain.dto.gemini.SummaryResultWithId
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.enums.Category
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import java.time.LocalDateTime

class PostDtoConverterTest : FunSpec({

    val converter = PostDtoConverter()
    val post = createMockPost("1")

    test("convert should update post with summary result") {
        val result = SummaryResultWithId(
            id = "1",
            success = true,
            summary = "New Summary",
            categories = listOf("AI", "BackEnd"),
            preview = "New Preview",
            error = null
        )

        val converted = converter.convert(post, result)

        converted.content shouldBe "New Summary"
        converted.preview shouldBe "New Preview"
        converted.categories shouldBe setOf(Category.AI, Category.BackEnd)
        converted.isSummary shouldBe true
    }

    test("convert should handle invalid categories gracefully") {
        val result = SummaryResultWithId(
            id = "1",
            success = true,
            summary = "Summary",
            categories = listOf("INVALID_CAT", "AI"),
            preview = "Preview",
            error = null
        )

        val converted = converter.convert(post, result)

        converted.categories shouldBe setOf(Category.AI)
    }

    test("convert should keep original content if summary is null") {
        val result = SummaryResultWithId(
            id = "1",
            success = true,
            summary = null,
            categories = emptyList(),
            preview = null,
            error = null
        )

        val converted = converter.convert(post, result)

        converted.content shouldBe post.content
    }
})

private fun createMockPost(id: String): PostDto {
    return PostDto(
        id = id,
        title = "Title $id",
        content = "Original Content",
        url = "http://url.com/$id",
        publishedAt = LocalDateTime.now(),
        company = mockk(),
        isSummary = false,
        preview = null,
        categories = emptySet(),
        isEmbedding = false
    )
}
