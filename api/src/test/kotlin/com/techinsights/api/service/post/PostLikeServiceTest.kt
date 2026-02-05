package com.techinsights.api.service.post

import com.techinsights.domain.entity.post.PostLike
import com.techinsights.domain.repository.post.PostLikeRepository
import com.techinsights.domain.repository.post.PostRepository
import com.techinsights.domain.utils.Tsid
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

    test("toggleLike - Authenticated User - New Like") {
        val postId = Tsid.generate()
        val userId = 100L
        val ipAddress = "127.0.0.1"

        every { postLikeRepository.findByPostIdAndUserId(any(), userId) } returns null
        every { postLikeRepository.save(any()) } returns mockk()
        every { postRepository.incrementLikeCount(any()) } returns Unit

        val result = postLikeService.toggleLike(postId = postId, userId = userId, ipAddress = ipAddress)

        result shouldBe true
        verify(exactly = 1) { postLikeRepository.save(any()) }
        verify(exactly = 1) { postRepository.incrementLikeCount(any()) }
    }

    test("toggleLike - Authenticated User - Already Liked (Unlike)") {
        val postId = Tsid.generate()
        val userId = 100L
        val ipAddress = "127.0.0.1"
        val existingLike = mockk<PostLike>()

        every { postLikeRepository.findByPostIdAndUserId(any(), userId) } returns existingLike
        every { postLikeRepository.deleteByPostIdAndUserId(any(), userId) } returns Unit
        every { postRepository.decrementLikeCount(any()) } returns Unit

        val result = postLikeService.toggleLike(postId = postId, userId = userId, ipAddress = ipAddress)

        result shouldBe false
        verify(exactly = 1) { postLikeRepository.deleteByPostIdAndUserId(any(), userId) }
        verify(exactly = 1) { postRepository.decrementLikeCount(any()) }
    }

    test("toggleLike - Anonymous User - New Like") {
        val postId = Tsid.generate()
        val ipAddress = "127.0.0.1"

        every { postLikeRepository.findByPostIdAndIpAddress(any(), ipAddress) } returns null
        every { postLikeRepository.save(any()) } returns mockk()
        every { postRepository.incrementLikeCount(any()) } returns Unit

        val result = postLikeService.toggleLike(postId = postId, userId = null, ipAddress = ipAddress)

        result shouldBe true
        verify(exactly = 1) { postLikeRepository.save(any()) }
        verify(exactly = 1) { postRepository.incrementLikeCount(any()) }
    }

    test("toggleLike - Anonymous User - Already Liked (Unlike)") {
        val postId = Tsid.generate()
        val ipAddress = "127.0.0.1"
        val existingLike = mockk<PostLike>()

        every { postLikeRepository.findByPostIdAndIpAddress(any(), ipAddress) } returns existingLike
        every { postLikeRepository.deleteByPostIdAndIpAddress(any(), ipAddress) } returns Unit
        every { postRepository.decrementLikeCount(any()) } returns Unit

        val result = postLikeService.toggleLike(postId = postId, userId = null, ipAddress = ipAddress)

        result shouldBe false
        verify(exactly = 1) { postLikeRepository.deleteByPostIdAndIpAddress(any(), ipAddress) }
        verify(exactly = 1) { postRepository.decrementLikeCount(any()) }
    }

    test("toggleLike - Concurrency Exception (Race Condition) - Authenticated") {
        val postId = Tsid.generate()
        val userId = 100L
        val ipAddress = "127.0.0.1"

        every { postLikeRepository.findByPostIdAndUserId(any(), userId) } returns null
        every { postLikeRepository.save(any()) } throws DataIntegrityViolationException("Duplicate key")
        
        val result = postLikeService.toggleLike(postId = postId, userId = userId, ipAddress = ipAddress)

        result shouldBe true
    }
})
