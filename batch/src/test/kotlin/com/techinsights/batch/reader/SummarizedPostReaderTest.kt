package com.techinsights.batch.reader

import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.repository.post.PostRepository
import com.techinsights.domain.utils.Tsid
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*
import org.springframework.batch.item.ExecutionContext
import java.time.LocalDateTime

class SummarizedPostReaderTest : FunSpec({

    lateinit var postRepository: PostRepository
    lateinit var reader: SummarizedPostReader

    val maxCount = 20L

    beforeEach {
        postRepository = mockk()
        reader = SummarizedPostReader(postRepository, maxCount)
    }

    afterEach {
        clearAllMocks()
    }

    test("read should fetch and return posts one by one") {
        // given
        val posts = createMockPosts(1, 3)
        every { postRepository.findOldestSummarizedAndNotEmbedded(any(), null, null) } returns posts
        every { postRepository.findOldestSummarizedAndNotEmbedded(any(), any(), 3L) } returns emptyList()

        // when & then
        reader.read()?.id shouldBe Tsid.encode(1L)
        reader.read()?.id shouldBe Tsid.encode(2L)
        reader.read()?.id shouldBe Tsid.encode(3L)
        reader.read() shouldBe null

        verify(exactly = 1) { postRepository.findOldestSummarizedAndNotEmbedded(any(), null, null) }
        verify(exactly = 1) { postRepository.findOldestSummarizedAndNotEmbedded(any(), any(), 3L) }
    }

    test("read should respect maxCount") {
        // given
        val readerWithLimit = SummarizedPostReader(postRepository, 2L)
        val posts = createMockPosts(1, 5)
        every { postRepository.findOldestSummarizedAndNotEmbedded(any(), any(), any()) } returns posts

        // when & then
        readerWithLimit.read() shouldNotBe null // 1
        readerWithLimit.read() shouldNotBe null // 2
        readerWithLimit.read() shouldBe null    // Limit reached
    }

    test("open and update should manage execution context") {
        // given
        val executionContext = ExecutionContext()
        val time = LocalDateTime.now()
        val id = 5L
        executionContext.putLong("summarizedPostReader.readCount", 10L)
        executionContext.putString("summarizedPostReader.cursor.publishedAt", time.toString())
        executionContext.putLong("summarizedPostReader.cursor.id", id)

        val newReader = SummarizedPostReader(postRepository, maxCount)
        every { postRepository.findOldestSummarizedAndNotEmbedded(any(), time, id) } returns emptyList()

        // when
        newReader.open(executionContext)
        newReader.read()

        // then
        verify { postRepository.findOldestSummarizedAndNotEmbedded(any(), time, id) }
    }

    test("read should return null if repository returns empty list") {
        // given
        every { postRepository.findOldestSummarizedAndNotEmbedded(any(), any(), any()) } returns emptyList()
        
        // when
        val result = reader.read()
        
        // then
        result shouldBe null
    }
})

private fun createMockPosts(startId: Long, count: Int): List<PostDto> {
    return (0 until count).map {
        val id = startId + it
        PostDto(
            id = Tsid.encode(id),
            title = "Title $id",
            preview = "Preview $id",
            url = "http://test.com/$id",
            content = "Content $id",
            publishedAt = LocalDateTime.now().minusDays(id),
            thumbnail = null,
            company = mockk(),
            viewCount = 0,
            categories = emptySet(),
            isSummary = true,
            isEmbedding = false
        )
    }
}
