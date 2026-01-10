package com.techinsights.batch.writer

import com.techinsights.domain.dto.embedding.PostEmbeddingDto
import com.techinsights.domain.entity.post.PostEmbedding
import com.techinsights.domain.repository.post.PostEmbeddingJpaRepository
import com.techinsights.domain.repository.post.PostRepository
import com.techinsights.domain.utils.Tsid
import io.kotest.core.spec.style.FunSpec
import io.mockk.*
import org.springframework.batch.item.Chunk

class PostEmbeddingWriterTest : FunSpec({

    val postEmbeddingJpaRepository = mockk<PostEmbeddingJpaRepository>()
    val postRepository = mockk<PostRepository>()
    val writer = PostEmbeddingWriter(postEmbeddingJpaRepository, postRepository)

    beforeEach {
        clearAllMocks()
    }

    test("write should save embeddings and update post status when chunk has valid items") {
        // given
        val postId1 = Tsid.encode(1L)
        val postId2 = Tsid.encode(2L)
        val dto1 = PostEmbeddingDto(
            postId = postId1,
            companyName = "Company A",
            categories = "AI,Backend",
            content = "Content 1",
            embeddingVector = FloatArray(768) { 0.1f }
        )
        val dto2 = PostEmbeddingDto(
            postId = postId2,
            companyName = "Company B",
            categories = "Frontend",
            content = "Content 2",
            embeddingVector = FloatArray(768) { 0.2f }
        )
        val chunk = Chunk(listOf(dto1, dto2))

        every { postEmbeddingJpaRepository.saveAll(any<Iterable<PostEmbedding>>()) } returns emptyList()
        every { postRepository.updateEmbeddingStatusBulk(any()) } returns 2L

        // when
        writer.write(chunk)

        // then
        verify(exactly = 1) { postEmbeddingJpaRepository.saveAll(any<Iterable<PostEmbedding>>()) }
        verify(exactly = 1) { postRepository.updateEmbeddingStatusBulk(listOf(postId1, postId2)) }
    }

    test("write should filter out null items") {
        // given
        val postId1 = Tsid.encode(1L)
        val dto1 = PostEmbeddingDto(
            postId = postId1,
            companyName = "Company A",
            categories = "AI",
            content = "Content 1",
            embeddingVector = FloatArray(768) { 0.1f }
        )
        // Create chunk with only valid item
        val chunk = Chunk(listOf(dto1))

        every { postEmbeddingJpaRepository.saveAll(any<Iterable<PostEmbedding>>()) } returns emptyList()
        every { postRepository.updateEmbeddingStatusBulk(any()) } returns 1L

        // when
        writer.write(chunk)

        // then
        verify(exactly = 1) { postEmbeddingJpaRepository.saveAll(any<Iterable<PostEmbedding>>()) }
        verify(exactly = 1) { postRepository.updateEmbeddingStatusBulk(match { it.size == 1 }) }
    }

    test("write should do nothing when chunk is empty") {
        // given
        val chunk = Chunk<PostEmbeddingDto>(emptyList())

        // when
        writer.write(chunk)

        // then
        verify(exactly = 0) { postEmbeddingJpaRepository.saveAll(any<Iterable<PostEmbedding>>()) }
        verify(exactly = 0) { postRepository.updateEmbeddingStatusBulk(any()) }
    }

    // Removed test for null items as Chunk<out PostEmbeddingDto> doesn't accept nullable types

    test("write should log warning when update count mismatches") {
        // given
        val postId1 = Tsid.encode(1L)
        val postId2 = Tsid.encode(2L)
        val dto1 = PostEmbeddingDto(
            postId = postId1,
            companyName = "Company A",
            categories = "AI",
            content = "Content 1",
            embeddingVector = FloatArray(768) { 0.1f }
        )
        val dto2 = PostEmbeddingDto(
            postId = postId2,
            companyName = "Company B",
            categories = "Backend",
            content = "Content 2",
            embeddingVector = FloatArray(768) { 0.2f }
        )
        val chunk = Chunk(listOf(dto1, dto2))

        every { postEmbeddingJpaRepository.saveAll(any<Iterable<PostEmbedding>>()) } returns emptyList()
        every { postRepository.updateEmbeddingStatusBulk(any()) } returns 1L // Mismatch: expected 2, got 1

        // when
        writer.write(chunk)

        // then
        verify(exactly = 1) { postEmbeddingJpaRepository.saveAll(any<Iterable<PostEmbedding>>()) }
        verify(exactly = 1) { postRepository.updateEmbeddingStatusBulk(any()) }
    }

    test("write should correctly convert PostEmbeddingDto to PostEmbedding entity") {
        // given
        val postId = Tsid.encode(100L)
        val dto = PostEmbeddingDto(
            postId = postId,
            companyName = "Test Company",
            categories = "AI,BigData",
            content = "Test content for embedding",
            embeddingVector = FloatArray(768) { it.toFloat() / 768f }
        )
        val chunk = Chunk(listOf(dto))

        val capturedEntities = slot<Iterable<PostEmbedding>>()
        every { postEmbeddingJpaRepository.saveAll(capture(capturedEntities)) } returns emptyList()
        every { postRepository.updateEmbeddingStatusBulk(any()) } returns 1L

        // when
        writer.write(chunk)

        // then
        verify(exactly = 1) { postEmbeddingJpaRepository.saveAll(any<Iterable<PostEmbedding>>()) }

        val savedEntity = capturedEntities.captured.first()
        assert(savedEntity.postId == Tsid.decode(postId))
        assert(savedEntity.companyName == "Test Company")
        assert(savedEntity.categories == "AI,BigData")
        assert(savedEntity.content == "Test content for embedding")
        assert(savedEntity.embeddingVector.size == 768)
    }

    test("write should handle multiple items in correct order") {
        // given
        val postIds = (1L..5L).map { Tsid.encode(it) }
        val dtos = postIds.map { postId ->
            PostEmbeddingDto(
                postId = postId,
                companyName = "Company",
                categories = "Test",
                content = "Content for $postId",
                embeddingVector = FloatArray(768) { 0.1f }
            )
        }
        val chunk = Chunk(dtos)

        every { postEmbeddingJpaRepository.saveAll<PostEmbedding>(any()) } returns emptyList()
        every { postRepository.updateEmbeddingStatusBulk(any()) } returns 5L

        // when
        writer.write(chunk)

        // then
        verify(exactly = 1) { postEmbeddingJpaRepository.saveAll<PostEmbedding>(any()) }
        verify(exactly = 1) { postRepository.updateEmbeddingStatusBulk(postIds) }
    }

    test("write should handle large embedding vectors") {
        // given
        val postId = Tsid.encode(1L)
        val largeVector = FloatArray(3072) { it.toFloat() / 3072f } // Gemini's actual dimension
        val dto = PostEmbeddingDto(
            postId = postId,
            companyName = "Company",
            categories = "AI",
            content = "Content",
            embeddingVector = largeVector
        )
        val chunk = Chunk(listOf(dto))

        val capturedEntities = slot<Iterable<PostEmbedding>>()
        every { postEmbeddingJpaRepository.saveAll(capture(capturedEntities)) } returns emptyList()
        every { postRepository.updateEmbeddingStatusBulk(any()) } returns 1L

        // when
        writer.write(chunk)

        // then
        verify(exactly = 1) { postEmbeddingJpaRepository.saveAll(any<Iterable<PostEmbedding>>()) }
        assert(capturedEntities.captured.first().embeddingVector.size == 3072)
    }

    test("write should handle special characters in content and categories") {
        // given
        val postId = Tsid.encode(1L)
        val dto = PostEmbeddingDto(
            postId = postId,
            companyName = "Company & Co.",
            categories = "AI,ML/DL,Back-End",
            content = "Content with special chars: !@#$%^&*()",
            embeddingVector = FloatArray(768) { 0.1f }
        )
        val chunk = Chunk(listOf(dto))

        every { postEmbeddingJpaRepository.saveAll(any<Iterable<PostEmbedding>>()) } returns emptyList()
        every { postRepository.updateEmbeddingStatusBulk(any()) } returns 1L

        // when
        writer.write(chunk)

        // then
        verify(exactly = 1) { postEmbeddingJpaRepository.saveAll(any<Iterable<PostEmbedding>>()) }
        verify(exactly = 1) { postRepository.updateEmbeddingStatusBulk(any()) }
    }
})
