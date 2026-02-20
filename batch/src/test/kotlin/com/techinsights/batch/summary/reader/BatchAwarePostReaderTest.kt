package com.techinsights.batch.summary.reader

import com.techinsights.batch.summary.builder.DynamicBatchBuilder
import com.techinsights.batch.summary.reader.validator.PostValidator
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

class BatchAwarePostReaderTest : FunSpec({

    lateinit var postRepository: PostRepository
    lateinit var batchBuilder: DynamicBatchBuilder
    lateinit var reader: BatchAwarePostReader

    beforeEach {
        postRepository = mockk()
        batchBuilder = mockk()
        mockkObject(PostValidator)
        every { PostValidator.isValidForSummary(any()) } returns true

        reader = BatchAwarePostReader(postRepository, batchBuilder, 100L)
    }

    afterEach {
        clearAllMocks()
        unmockkObject(PostValidator)
    }

    test("read should return null when max count is reached") {
        // given
        reader = BatchAwarePostReader(postRepository, batchBuilder, 0L)

        // when
        val result = reader.read()

        // then
        result.shouldBeNull()
        verify(exactly = 0) { postRepository.findOldestNotSummarized(any(), any(), any()) }
    }

    test("read should return posts in batches") {
        // given
        val posts = listOf(createPostDto(1L, "Title 1"), createPostDto(2L, "Title 2"))
        val batch = DynamicBatchBuilder.Batch(posts, 1000)

        every { postRepository.findOldestNotSummarized(any(), any(), any()) } returns emptyList()
        every { postRepository.findOldestNotSummarized(any(), null, null) } returns posts

        every { batchBuilder.buildBatches(posts) } returns listOf(batch)

        reader.open(ExecutionContext())

        // when
        val result = reader.read()

        // then
        result.shouldNotBeNull()
        result shouldHaveSize 2
        result.map { it.title } shouldBe listOf("Title 1", "Title 2")
    }

    test("read should filter out invalid posts based on Validator") {
        // given
        val validPost = createPostDto(1L, "Valid Post")
        val invalidPost = createPostDto(2L, "Invalid Post")
        val posts = listOf(validPost, invalidPost)

        every { postRepository.findOldestNotSummarized(any(), any(), any()) } returns emptyList()
        every { postRepository.findOldestNotSummarized(any(), null, null) } returns posts

        every { PostValidator.isValidForSummary(validPost) } returns true
        every { PostValidator.isValidForSummary(invalidPost) } returns false

        every { batchBuilder.buildBatches(listOf(validPost)) } returns listOf(
            DynamicBatchBuilder.Batch(listOf(validPost), 500)
        )

        reader.open(ExecutionContext())

        // when
        val result = reader.read()

        // then
        result.shouldNotBeNull()
        result shouldHaveSize 1
        result[0].title shouldBe "Valid Post"
    }

    test("read should return null when all posts are filtered out") {
        // given
        val posts = listOf(createPostDto(1L, "Invalid Post"))

        every { postRepository.findOldestNotSummarized(any(), any(), any()) } returns emptyList()
        every { postRepository.findOldestNotSummarized(any(), null, null) } returns posts
        every { PostValidator.isValidForSummary(any()) } returns false

        reader.open(ExecutionContext())

        // when
        val result = reader.read()

        // then
        result.shouldBeNull()
    }

    test("read should handle multiple batches from same fetch") {
        // given
        val posts = (1..5).map { createPostDto(it.toLong(), "Title $it") }
        val batch1 = DynamicBatchBuilder.Batch(posts.take(3), 1500)
        val batch2 = DynamicBatchBuilder.Batch(posts.drop(3), 1000)

        every { postRepository.findOldestNotSummarized(any(), any(), any()) } returns emptyList()
        every { postRepository.findOldestNotSummarized(any(), null, null) } returns posts
        every { batchBuilder.buildBatches(posts) } returns listOf(batch1, batch2)

        reader.open(ExecutionContext())

        // when & then
        reader.read().shouldNotBeNull() shouldHaveSize 3
        reader.read().shouldNotBeNull() shouldHaveSize 2
        reader.read().shouldBeNull() // 데이터 소진 후 DB 재조회 결과가 empty이므로 null
    }

    test("open should restore cursor from execution context") {
        // given
        val executionContext = ExecutionContext()
        val savedTime = LocalDateTime.of(2024, 1, 1, 12, 0)
        val savedId = 12345L

        executionContext.putString("batchAwarePostReader.cursor.publishedAt", savedTime.toString())
        executionContext.putLong("batchAwarePostReader.cursor.id", savedId)

        val posts = listOf(createPostDto(1L, "Restored Post"))

        every { postRepository.findOldestNotSummarized(any(), any(), any()) } returns emptyList()
        every { postRepository.findOldestNotSummarized(any(), savedTime, savedId) } returns posts
        every { batchBuilder.buildBatches(posts) } returns listOf(DynamicBatchBuilder.Batch(posts, 500))

        // when
        reader.open(executionContext)
        val result = reader.read()

        // then
        result.shouldNotBeNull()
        verify(exactly = 1) { postRepository.findOldestNotSummarized(any(), savedTime, savedId) }
    }

    test("update should save cursor to execution context") {
        // given
        val post = createPostDto(99L, "Last Post")
        val posts = listOf(post)

        every { postRepository.findOldestNotSummarized(any(), any(), any()) } returns emptyList()
        every { postRepository.findOldestNotSummarized(any(), null, null) } returns posts
        every { batchBuilder.buildBatches(posts) } returns listOf(DynamicBatchBuilder.Batch(posts, 500))

        val executionContext = ExecutionContext()
        reader.open(executionContext)

        // when
        reader.read()
        reader.update(executionContext)

        // then
        executionContext.getString("batchAwarePostReader.cursor.publishedAt") shouldBe post.publishedAt.toString()
        executionContext.getLong("batchAwarePostReader.cursor.id") shouldBe 99L
        executionContext.getLong("batchAwarePostReader.readCount") shouldBe 1L
    }

    test("read should respect maxCount limit") {
        // given
        val limit = 2L
        reader = BatchAwarePostReader(postRepository, batchBuilder, limit)

        val posts = (1..3).map { createPostDto(it.toLong(), "Title $it") }
        val fetchedPosts = posts.take(2)
        val batch = DynamicBatchBuilder.Batch(fetchedPosts, 1000)

        every { postRepository.findOldestNotSummarized(any(), any(), any()) } returns emptyList()
        every { postRepository.findOldestNotSummarized(limit, null, null) } returns fetchedPosts
        every { batchBuilder.buildBatches(fetchedPosts) } returns listOf(batch)

        reader.open(ExecutionContext())

        // when
        val result1 = reader.read()
        val result2 = reader.read()

        // then
        result1.shouldNotBeNull() shouldHaveSize 2
        result2.shouldBeNull()
    }
})

private fun createPostDto(
    id: Long,
    title: String
): PostDto {
    return PostDto(
        id = Tsid.encode(id),
        title = title,
        content = "This is a long enough content for testing purposes.",
        url = "https://example.com/$id",
        publishedAt = LocalDateTime.now().withNano(0), // 비교 시 나노초 차이 방지
        company = CompanyDto("c1", "TechInsights", "", ""),
        isSummary = false,
        preview = null,
        categories = emptySet(),
        isEmbedding = false
    )
}