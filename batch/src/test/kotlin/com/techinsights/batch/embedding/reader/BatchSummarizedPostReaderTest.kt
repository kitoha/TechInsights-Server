package com.techinsights.batch.embedding.reader

import com.techinsights.domain.dto.company.CompanyDto
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.repository.post.PostRepository
import com.techinsights.domain.utils.Tsid
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.springframework.batch.item.ExecutionContext
import java.time.LocalDateTime

class BatchSummarizedPostReaderTest : FunSpec({

    lateinit var postRepository: PostRepository
    lateinit var reader: BatchSummarizedPostReader

    beforeEach {
        postRepository = mockk()
        reader = BatchSummarizedPostReader(postRepository, 1000L, 50)
    }

    afterEach {
        clearAllMocks()
    }

    test("read should return posts batch") {
        // given
        val posts = listOf(createPostDto(1L, "Title 1", isSummary = true))

        every { postRepository.findOldestSummarizedAndNotEmbedded(any(), any(), any()) } returns emptyList()
        every { postRepository.findOldestSummarizedAndNotEmbedded(50L, null, null) } returns posts

        reader.open(ExecutionContext())

        // when
        val result = reader.read()

        // then
        result.shouldNotBeNull() shouldHaveSize 1
        verify(exactly = 1) { postRepository.findOldestSummarizedAndNotEmbedded(50L, null, null) }
    }

    test("read should return null when max count is reached") {
        // given
        reader = BatchSummarizedPostReader(postRepository, 0L, 50)

        // when
        val result = reader.read()

        // then
        result.shouldBeNull()
        verify(exactly = 0) { postRepository.findOldestSummarizedAndNotEmbedded(any(), any(), any()) }
    }

    test("read should respect remaining count when less than batch size") {
        val maxCount = 100L
        reader = BatchSummarizedPostReader(postRepository, maxCount, 50)

        val firstBatch = (1..50).map { createPostDto(it.toLong(), "T$it", isSummary = true) }
        val secondBatch = (51..100).map { createPostDto(it.toLong(), "T$it", isSummary = true) }

        every { postRepository.findOldestSummarizedAndNotEmbedded(any(), any(), any()) } returns emptyList()
        every { postRepository.findOldestSummarizedAndNotEmbedded(50L, null, null) } returns firstBatch
        every { postRepository.findOldestSummarizedAndNotEmbedded(50L, any(), any()) } returns secondBatch

        reader.open(ExecutionContext())

        // when
        reader.read()
        reader.read()
        val result = reader.read()

        // then
        result.shouldBeNull()
    }

    test("read should update cursor after reading batch") {
        // given
        val publishedAt = LocalDateTime.of(2024, 1, 1, 12, 0)
        val post = createPostDto(1L, "Title 1", isSummary = true, publishedAt = publishedAt)

        every { postRepository.findOldestSummarizedAndNotEmbedded(any(), any(), any()) } returns emptyList()
        every { postRepository.findOldestSummarizedAndNotEmbedded(50L, null, null) } returns listOf(post)

        reader.open(ExecutionContext())

        // when
        reader.read()
        val result = reader.read()

        // then
        result.shouldBeNull()
        verify { postRepository.findOldestSummarizedAndNotEmbedded(50L, publishedAt, 1L) }
    }

    test("open should restore cursor from execution context") {
        // given
        val executionContext = ExecutionContext()
        val savedTime = LocalDateTime.of(2024, 1, 1, 12, 0)
        val savedId = 12345L

        executionContext.putString("batchSummarizedPostReader.cursor.publishedAt", savedTime.toString())
        executionContext.putLong("batchSummarizedPostReader.cursor.id", savedId)

        every { postRepository.findOldestSummarizedAndNotEmbedded(any(), any(), any()) } returns emptyList()
        every { postRepository.findOldestSummarizedAndNotEmbedded(50L, savedTime, savedId) } returns listOf(createPostDto(1L, "T"))

        // when
        reader.open(executionContext)
        reader.read()

        // then
        verify(exactly = 1) { postRepository.findOldestSummarizedAndNotEmbedded(50L, savedTime, savedId) }
    }

    test("update should save cursor to execution context") {
        // given
        val post = createPostDto(99L, "Title", isSummary = true)
        every { postRepository.findOldestSummarizedAndNotEmbedded(any(), any(), any()) } returns listOf(post)

        val executionContext = ExecutionContext()
        reader.open(executionContext)

        // when
        reader.read()
        reader.update(executionContext)

        // then
        executionContext.getString("batchSummarizedPostReader.cursor.publishedAt") shouldBe post.publishedAt.toString()
        executionContext.getLong("batchSummarizedPostReader.cursor.id") shouldBe 99L
        executionContext.getLong("batchSummarizedPostReader.readCount") shouldBe 1L
    }

    test("read should handle multiple sequential reads correctly") {
        // given
        val batch1 = (1..50).map { createPostDto(it.toLong(), "T$it", isSummary = true) }
        val batch2 = (51..100).map { createPostDto(it.toLong(), "T$it", isSummary = true) }

        every { postRepository.findOldestSummarizedAndNotEmbedded(any(), any(), any()) } returnsMany listOf(batch1, batch2, emptyList())

        reader.open(ExecutionContext())

        // when
        val result1 = reader.read()
        val result2 = reader.read()
        val result3 = reader.read()

        // then
        result1.shouldNotBeNull() shouldHaveSize 50
        result1[0].id shouldBe Tsid.encode(1L)

        result2.shouldNotBeNull() shouldHaveSize 50
        result2[0].id shouldBe Tsid.encode(51L)

        result3.shouldBeNull()
    }

    test("read should stop when max count reached in middle of reading") {
        // given
        reader = BatchSummarizedPostReader(postRepository, 75L, 50)

        val batch1 = (1..50).map { createPostDto(it.toLong(), "T$it", isSummary = true) }
        val batch2 = (51..75).map { createPostDto(it.toLong(), "T$it", isSummary = true) }

        every { postRepository.findOldestSummarizedAndNotEmbedded(50L, any(), any()) } returns batch1
        every { postRepository.findOldestSummarizedAndNotEmbedded(25L, any(), any()) } returns batch2

        reader.open(ExecutionContext())

        // when
        val result1 = reader.read()
        val result2 = reader.read()
        val result3 = reader.read()

        // then
        result1.shouldNotBeNull() shouldHaveSize 50
        result2.shouldNotBeNull() shouldHaveSize 25
        result3.shouldBeNull()
    }
})

private fun createPostDto(
    id: Long,
    title: String,
    isSummary: Boolean = false,
    publishedAt: LocalDateTime = LocalDateTime.now().withNano(0)
): PostDto {
    return PostDto(
        id = Tsid.encode(id),
        title = title,
        content = "Content",
        url = "https://example.com/$id",
        publishedAt = publishedAt,
        company = CompanyDto(
            id = "company-1",
            name = "Test Company",
            blogUrl = "https://example.com/rss",
            logoImageName = ""
        ),
        isSummary = isSummary,
        preview = if (isSummary) "Preview $id" else null,
        categories = emptySet(),
        isEmbedding = false
    )
}