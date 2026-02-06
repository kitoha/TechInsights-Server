package com.techinsights.api.service.post

import com.techinsights.domain.dto.auth.Requester
import com.techinsights.domain.entity.post.PostLike
import com.techinsights.domain.exception.PostNotFoundException
import com.techinsights.domain.repository.post.PostLikeRepository
import com.techinsights.domain.repository.post.PostRepository
import com.techinsights.domain.utils.Tsid
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.dao.DataIntegrityViolationException

class PostLikeServiceTest : FunSpec({

    val postLikeRepository = mockk<PostLikeRepository>()
    val postRepository = mockk<PostRepository>()
    val postLikeService = PostLikeService(postLikeRepository, postRepository)

    beforeTest {
        io.mockk.clearAllMocks()
    }

    test("toggleLike - Post Not Found") {
        val postId = Tsid.generate()
        val userId = 100L
        val ipAddress = "127.0.0.1"
        val requester = Requester.Authenticated(userId, ipAddress)

        every { postRepository.existsById(any()) } returns false

        shouldThrow<PostNotFoundException> {
            postLikeService.toggleLike(postId, requester)
        }
    }

    test("toggleLike - Authenticated User - New Like") {
        val postId = Tsid.generate()
        val userId = 100L
        val ipAddress = "127.0.0.1"
        val requester = Requester.Authenticated(userId, ipAddress)

        every { postRepository.existsById(any()) } returns true
        every { postLikeRepository.findByPostIdAndUserId(any(), userId) } returns null
        every { postLikeRepository.save(any()) } returns mockk()
        every { postRepository.incrementLikeCount(any()) } returns Unit

        val result = postLikeService.toggleLike(postId, requester)

        result shouldBe true
        verify(exactly = 1) { postLikeRepository.save(any()) }
        verify(exactly = 1) { postRepository.incrementLikeCount(any()) }
    }

    test("toggleLike - Authenticated User - Already Liked (Unlike)") {
        val postIdStr = Tsid.generate()
        val postId = Tsid.decode(postIdStr)
        val userId = 100L
        val ipAddress = "127.0.0.1"
        val requester = Requester.Authenticated(userId, ipAddress)
        val existingLike = mockk<PostLike>()

        every { postRepository.existsById(any()) } returns true
        every { postLikeRepository.findByPostIdAndUserId(postId, userId) } returns existingLike
        every { postLikeRepository.deleteByPostIdAndUserId(postId, userId) } returns 1L
        every { postRepository.decrementLikeCount(postId) } returns Unit

        val result = postLikeService.toggleLike(postIdStr, requester)

        result shouldBe false
        verify(exactly = 1) { postLikeRepository.deleteByPostIdAndUserId(postId, userId) }
        verify(exactly = 1) { postRepository.decrementLikeCount(postId) }
    }

    test("toggleLike - Anonymous User - New Like") {
        val postIdStr = Tsid.generate()
        val postId = Tsid.decode(postIdStr)
        val ipAddress = "127.0.0.1"
        val requester = Requester.Anonymous(ipAddress, ipAddress)

        every { postRepository.existsById(any()) } returns true
        every { postLikeRepository.findAnonymousByPostIdAndIpAddress(postId, ipAddress) } returns null
        every { postLikeRepository.save(any()) } returns mockk()
        every { postRepository.incrementLikeCount(postId) } returns Unit

        val result = postLikeService.toggleLike(postIdStr, requester)

        result shouldBe true
        verify(exactly = 1) { postLikeRepository.save(any()) }
        verify(exactly = 1) { postRepository.incrementLikeCount(postId) }
    }

    test("toggleLike - Anonymous User - Already Liked (Unlike)") {
        val postIdStr = Tsid.generate()
        val postId = Tsid.decode(postIdStr)
        val ipAddress = "127.0.0.1"
        val requester = Requester.Anonymous(ipAddress, ipAddress)
        val existingLike = mockk<PostLike>()

        every { postRepository.existsById(any()) } returns true
        every { postLikeRepository.findAnonymousByPostIdAndIpAddress(postId, ipAddress) } returns existingLike
        every { postLikeRepository.deleteAnonymousByPostIdAndIpAddress(postId, ipAddress) } returns 1L
        every { postRepository.decrementLikeCount(postId) } returns Unit

        val result = postLikeService.toggleLike(postIdStr, requester)

        result shouldBe false
        verify(exactly = 1) { postLikeRepository.deleteAnonymousByPostIdAndIpAddress(postId, ipAddress) }
        verify(exactly = 1) { postRepository.decrementLikeCount(postId) }
    }

    test("toggleLike - Office IP Scenario - Different Users Same IP") {
        val postIdStr = Tsid.generate()
        val postId = Tsid.decode(postIdStr)
        val ipAddress = "1.2.3.4"
        val userA = Requester.Authenticated(101L, ipAddress)
        val userB = Requester.Authenticated(102L, ipAddress)

        every { postRepository.existsById(any()) } returns true
        every { postLikeRepository.findByPostIdAndUserId(postId, 101L) } returns null
        every { postLikeRepository.findByPostIdAndUserId(postId, 102L) } returns null
        every { postLikeRepository.save(any()) } returns mockk()
        every { postRepository.incrementLikeCount(postId) } returns Unit

        postLikeService.toggleLike(postIdStr, userA) shouldBe true

        postLikeService.toggleLike(postIdStr, userB) shouldBe true

        verify(exactly = 2) { postLikeRepository.save(any()) }
        verify(exactly = 2) { postRepository.incrementLikeCount(postId) }
    }

    test("toggleLike - Concurrency Exception (Race Condition) - Authenticated - Duplicate Found") {
        val postIdStr = Tsid.generate()
        val postId = Tsid.decode(postIdStr)
        val userId = 100L
        val ipAddress = "127.0.0.1"
        val requester = Requester.Authenticated(userId, ipAddress)

        every { postRepository.existsById(any()) } returns true
        every { postLikeRepository.findByPostIdAndUserId(postId, userId) } returnsMany listOf(null, mockk())
        every { postLikeRepository.save(any()) } throws DataIntegrityViolationException("Duplicate key")

        val result = postLikeService.toggleLike(postIdStr, requester)

        result shouldBe false // saveLikeIfAbsent returns false
        verify(exactly = 0) { postRepository.incrementLikeCount(any()) }
    }

    test("toggleLike - Unlike Race Condition - Already Deleted") {
        val postIdStr = Tsid.generate()
        val postId = Tsid.decode(postIdStr)
        val userId = 100L
        val ipAddress = "127.0.0.1"
        val requester = Requester.Authenticated(userId, ipAddress)
        val existingLike = mockk<PostLike>()

        every { postRepository.existsById(any()) } returns true
        every { postLikeRepository.findByPostIdAndUserId(postId, userId) } returns existingLike
        every { postLikeRepository.deleteByPostIdAndUserId(postId, userId) } returns 0L // Already deleted by someone else
        
        val result = postLikeService.toggleLike(postIdStr, requester)

        result shouldBe false
        verify(exactly = 0) { postRepository.decrementLikeCount(any()) }
    }
})
