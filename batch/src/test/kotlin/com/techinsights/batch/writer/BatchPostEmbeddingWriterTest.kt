package com.techinsights.batch.writer

import com.techinsights.domain.dto.embedding.PostEmbeddingDto
import com.techinsights.domain.entity.post.PostEmbedding
import com.techinsights.domain.repository.post.PostEmbeddingJpaRepository
import com.techinsights.domain.repository.post.PostRepository
import com.techinsights.domain.utils.Tsid
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.*
import org.springframework.batch.item.Chunk

class BatchPostEmbeddingWriterTest : FunSpec({

    lateinit var postEmbeddingJpaRepository: PostEmbeddingJpaRepository
    lateinit var postRepository: PostRepository
    lateinit var writer: BatchPostEmbeddingWriter

    val validTsidId1 = Tsid.encode(1L)
    val validTsidId2 = Tsid.encode(2L)
    val validTsidId3 = Tsid.encode(3L)

    beforeEach {
        postEmbeddingJpaRepository = mockk()
        postRepository = mockk()
        writer = BatchPostEmbeddingWriter(postEmbeddingJpaRepository, postRepository)
    }

    afterEach {
        clearAllMocks()
    }

    test("write should successfully save embeddings and update post status") {
        // given
        val embeddings = listOf(
            createEmbeddingDto(validTsidId1, "Company", "AI", "Content 1"),
            createEmbeddingDto(validTsidId2, "Company", "Backend", "Content 2")
        )
        val chunk = Chunk(listOf(embeddings))

        every { postEmbeddingJpaRepository.saveAll(any<List<PostEmbedding>>()) } returns emptyList()
        every { postRepository.updateEmbeddingStatusBulk(any<List<String>>()) } returns 2L

        // when
        writer.write(chunk)

        // then
        verify(exactly = 1) { postEmbeddingJpaRepository.saveAll(any<List<PostEmbedding>>()) }
        verify(exactly = 1) { postRepository.updateEmbeddingStatusBulk(listOf(validTsidId1, validTsidId2)) }
    }

    test("write should handle empty chunk") {
        // given
        val chunk = Chunk<List<PostEmbeddingDto>>(emptyList())

        // when
        writer.write(chunk)

        // then
        verify(exactly = 0) { postEmbeddingJpaRepository.saveAll(any<List<PostEmbedding>>()) }
        verify(exactly = 0) { postRepository.updateEmbeddingStatusBulk(any<List<String>>()) }
    }

    test("write should flatten nested lists") {
        // given
        val batch1 = listOf(
            createEmbeddingDto(validTsidId1, "Company", "AI", "Content 1")
        )
        val batch2 = listOf(
            createEmbeddingDto(validTsidId2, "Company", "Backend", "Content 2"),
            createEmbeddingDto(validTsidId3, "Company", "DevOps", "Content 3")
        )
        val chunk = Chunk(listOf(batch1, batch2))

        every { postEmbeddingJpaRepository.saveAll(any<List<PostEmbedding>>()) } returns emptyList()
        every { postRepository.updateEmbeddingStatusBulk(any<List<String>>()) } returns 3L

        // when
        writer.write(chunk)

        // then
        verify(exactly = 1) { postEmbeddingJpaRepository.saveAll(any<List<PostEmbedding>>()) }
        verify(exactly = 1) { postRepository.updateEmbeddingStatusBulk(any<List<String>>()) }
    }

    test("write should log warning when update count doesn't match") {
        // given
        val embeddings = listOf(
            createEmbeddingDto(validTsidId1, "Company", "AI", "Content 1"),
            createEmbeddingDto(validTsidId2, "Company", "Backend", "Content 2")
        )
        val chunk = Chunk(listOf(embeddings))

        every { postEmbeddingJpaRepository.saveAll(any<List<PostEmbedding>>()) } returns emptyList()
        every { postRepository.updateEmbeddingStatusBulk(any<List<String>>()) } returns 1L

        // when
        writer.write(chunk)

        // then
        verify(exactly = 1) { postEmbeddingJpaRepository.saveAll(any<List<PostEmbedding>>()) }
        verify(exactly = 1) { postRepository.updateEmbeddingStatusBulk(any<List<String>>()) }
    }

    test("write should propagate exception from saveAll") {
        // given
        val embeddings = listOf(createEmbeddingDto(validTsidId1, "Company", "AI", "Content"))
        val chunk = Chunk(listOf(embeddings))

        every { postEmbeddingJpaRepository.saveAll(any<List<PostEmbedding>>()) } throws RuntimeException("DB error")

        // when / then
        val exception = runCatching { writer.write(chunk) }.exceptionOrNull()
        exception.shouldBeInstanceOf<RuntimeException>()

        verify(exactly = 1) { postEmbeddingJpaRepository.saveAll(any<List<PostEmbedding>>()) }
        verify(exactly = 0) { postRepository.updateEmbeddingStatusBulk(any<List<String>>()) }
    }

    test("write should propagate exception from updateEmbeddingStatusBulk") {
        // given
        val embeddings = listOf(createEmbeddingDto(validTsidId1, "Company", "AI", "Content"))
        val chunk = Chunk(listOf(embeddings))

        every { postEmbeddingJpaRepository.saveAll(any<List<PostEmbedding>>()) } returns emptyList()
        every { postRepository.updateEmbeddingStatusBulk(any<List<String>>()) } throws RuntimeException("Update error")

        // when / then
        val exception = runCatching { writer.write(chunk) }.exceptionOrNull()
        exception.shouldBeInstanceOf<RuntimeException>()

        verify(exactly = 1) { postEmbeddingJpaRepository.saveAll(any<List<PostEmbedding>>()) }
        verify(exactly = 1) { postRepository.updateEmbeddingStatusBulk(any<List<String>>()) }
    }

    test("write should correctly map PostEmbeddingDto to PostEmbedding entity") {
        // given
        val embedding = createEmbeddingDto(
            postId = validTsidId1,
            companyName = "Tech Corp",
            categories = "AI,Backend,DevOps",
            content = "Test content for embedding"
        )
        val chunk = Chunk(listOf(listOf(embedding)))

        val capturedEntity = slot<List<PostEmbedding>>()
        every { postEmbeddingJpaRepository.saveAll(capture(capturedEntity)) } returns emptyList()
        every { postRepository.updateEmbeddingStatusBulk(any<List<String>>()) } returns 1L

        // when
        writer.write(chunk)

        // then
        val savedEntity = capturedEntity.captured[0]
        savedEntity.companyName shouldBe "Tech Corp"
        savedEntity.categories shouldBe "AI,Backend,DevOps"
        savedEntity.content shouldBe "Test content for embedding"
        savedEntity.embeddingVector.size shouldBe 3072
    }

    test("write should handle multiple batches with different sizes") {
        // given
        val batch1 = (1..5).map { createEmbeddingDto(Tsid.encode(it.toLong()), "Company", "AI", "Content $it") }
        val batch2 = (6..8).map { createEmbeddingDto(Tsid.encode(it.toLong()), "Company", "Backend", "Content $it") }
        val batch3 = listOf(createEmbeddingDto(Tsid.encode(9L), "Company", "DevOps", "Content 9"))
        val chunk = Chunk(listOf(batch1, batch2, batch3))

        every { postEmbeddingJpaRepository.saveAll(any<List<PostEmbedding>>()) } returns emptyList()
        every { postRepository.updateEmbeddingStatusBulk(any<List<String>>()) } returns 9L

        // when
        writer.write(chunk)

        // then
        verify(exactly = 1) { postEmbeddingJpaRepository.saveAll(any<List<PostEmbedding>>()) }
        verify(exactly = 1) { postRepository.updateEmbeddingStatusBulk(any<List<String>>()) }
    }

    test("write should preserve embedding vector data") {
        // given
        val customVector = FloatArray(3072) { it.toFloat() * 0.001f }
        val embedding = PostEmbeddingDto(
            postId = validTsidId1,
            companyName = "Company",
            categories = "AI",
            content = "Content",
            embeddingVector = customVector
        )
        val chunk = Chunk(listOf(listOf(embedding)))

        val capturedEntity = slot<List<PostEmbedding>>()
        every { postEmbeddingJpaRepository.saveAll(capture(capturedEntity)) } returns emptyList()
        every { postRepository.updateEmbeddingStatusBulk(any<List<String>>()) } returns 1L

        // when
        writer.write(chunk)

        // then
        val savedEntity = capturedEntity.captured[0]
        savedEntity.embeddingVector.size shouldBe 3072
        savedEntity.embeddingVector[0] shouldBe 0.0f
        savedEntity.embeddingVector[100] shouldBe 0.1f
    }
})

private fun createEmbeddingDto(
    postId: String,
    companyName: String,
    categories: String,
    content: String
): PostEmbeddingDto {
    return PostEmbeddingDto(
        postId = postId,
        companyName = companyName,
        categories = categories,
        content = content,
        embeddingVector = FloatArray(3072) { it.toFloat() }
    )
}
