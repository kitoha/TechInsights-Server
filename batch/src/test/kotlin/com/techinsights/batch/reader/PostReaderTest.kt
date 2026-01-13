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

class PostReaderTest : FunSpec({

    lateinit var postRepository: PostRepository
    lateinit var reader: PostReader

    val maxCount = 50L

    beforeEach {
        postRepository = mockk()
        reader = PostReader(postRepository, maxCount)
    }

    afterEach {
        clearAllMocks()
    }

    test("read should fetch and return posts one by one") {
        // given
        val posts = createMockPosts(1, 3)
        every { postRepository.findOldestNotSummarized(any(), null, null) } returns posts
        every { postRepository.findOldestNotSummarized(any(), any(), 3L) } returns emptyList()

        reader.open(ExecutionContext())

        // when & then
        val post1 = reader.read()
        post1?.id shouldBe Tsid.encode(1L)

        val post2 = reader.read()
        post2?.id shouldBe Tsid.encode(2L)

        val post3 = reader.read()
        post3?.id shouldBe Tsid.encode(3L)

        val post4 = reader.read()
        post4 shouldBe null

        verify(exactly = 1) { postRepository.findOldestNotSummarized(any(), null, null) }
        verify(exactly = 1) { postRepository.findOldestNotSummarized(any(), any(), 3L) }
    }
    
    test("read should filter invalid posts") {
        // given
        val validPost1 = createMockPost(1)
        val invalidPost = createMockPost(2, content = "short")
        val validPost2 = createMockPost(3)
        val posts = listOf(validPost1, invalidPost, validPost2)
        
        every { postRepository.findOldestNotSummarized(any(), any(), any()) } returns posts andThen emptyList()
        
        // when & then
        val p1 = reader.read()
        p1?.id shouldBe Tsid.encode(1L)
        
        val p2 = reader.read()
        p2?.id shouldBe Tsid.encode(3L)
        
        val p3 = reader.read()
        p3 shouldBe null
    }
    
    test("read should respect maxCount") {
        // given
        val readerWithLimit = PostReader(postRepository, 2L)
        val posts = createMockPosts(1, 5)
        every { postRepository.findOldestNotSummarized(any(), any(), any()) } returns posts
        
        // when & then
        readerWithLimit.read() shouldNotBe null // 1
        readerWithLimit.read() shouldNotBe null // 2
        readerWithLimit.read() shouldBe null    // Limit reached
    }

    test("open and update should manage execution context") {
        // given
        val executionContext = ExecutionContext()
        val posts = createMockPosts(1, 3)
        every { postRepository.findOldestNotSummarized(any(), null, null) } returns posts

        // when
        reader.open(executionContext)
        reader.read()
        reader.update(executionContext)

        val savedContext = ExecutionContext()
        savedContext.putLong("postReader.readCount", 1L)
        val time = LocalDateTime.now().toString()
        savedContext.putString("postReader.cursor.publishedAt", time)
        savedContext.putLong("postReader.cursor.id", 1L)

        val newReader = PostReader(postRepository, maxCount)
        every { postRepository.findOldestNotSummarized(any(), any(), 1L) } returns emptyList()

        // when
        newReader.open(savedContext)
        newReader.read() // Attempt to read from restored state

        // then
        verify { postRepository.findOldestNotSummarized(any(), LocalDateTime.parse(time), 1L) }
    }
})

private fun createMockPosts(startId: Long, count: Int): List<PostDto> {
    return (0 until count).map {
        createMockPost(startId + it, "Valid content for post ${startId + it}".repeat(10))
    }
}

private fun createMockPost(id: Long, content: String = "default content".repeat(10)): PostDto {
    return PostDto(
        id = Tsid.encode(id),
        title = "Title $id",
        preview = "Preview $id",
        url = "http://test.com/$id",
        content = content,
        publishedAt = LocalDateTime.now().minusDays(id),
        thumbnail = null,
        company = mockk(),
        viewCount = 0,
        categories = emptySet(),
        isSummary = false,
        isEmbedding = false
    )
}
