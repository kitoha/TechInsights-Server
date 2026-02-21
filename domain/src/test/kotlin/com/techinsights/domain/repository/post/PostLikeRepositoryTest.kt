package com.techinsights.domain.repository.post

import com.techinsights.domain.entity.post.PostLike
import com.techinsights.domain.utils.Tsid
import com.techinsights.domain.utils.decode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class PostLikeRepositoryTest : FunSpec({

    val postLikeJpaRepository = mockk<PostLikeJpaRepository>()
    val postLikeRepository = PostLikeRepositoryImpl(postLikeJpaRepository)

    test("saving a new PostLike should delegate to JpaRepository") {
        val postId = Tsid.generate().decode()
        val userId = 100L
        val ipAddress = "192.168.0.1"

        val postLike = PostLike(
            id = Tsid.generate().decode(),
            postId = postId,
            userId = userId,
            ipAddress = ipAddress
        )

        every { postLikeJpaRepository.save(postLike) } returns postLike

        val saved = postLikeRepository.save(postLike)

        saved shouldBe postLike
        verify(exactly = 1) { postLikeJpaRepository.save(postLike) }
    }

    test("finding by postId and userId should delegate to JpaRepository") {
        val postId = Tsid.generate().decode()
        val userId = 100L
        val postLike = mockk<PostLike>()

        every { postLikeJpaRepository.findByPostIdAndUserId(postId, userId) } returns postLike

        val result = postLikeRepository.findByPostIdAndUserId(postId, userId)

        result shouldBe postLike
        verify(exactly = 1) { postLikeJpaRepository.findByPostIdAndUserId(postId, userId) }
    }

    test("finding by postId and ipAddress should delegate to JpaRepository") {
        val postId = Tsid.generate().decode()
        val ipAddress = "127.0.0.1"
        val postLike = mockk<PostLike>()

        every { postLikeJpaRepository.findByPostIdAndIpAddress(postId, ipAddress) } returns postLike

        val result = postLikeRepository.findByPostIdAndIpAddress(postId, ipAddress)

        result shouldBe postLike
        verify(exactly = 1) { postLikeJpaRepository.findByPostIdAndIpAddress(postId, ipAddress) }
    }

    test("deleteByPostIdAndUserId should delegate to JpaRepository and return count") {
        val postId = Tsid.generate().decode()
        val userId = 100L

        every { postLikeJpaRepository.deleteByPostIdAndUserId(postId, userId) } returns 1L

        val result = postLikeRepository.deleteByPostIdAndUserId(postId, userId)

        result shouldBe 1L
        verify(exactly = 1) { postLikeJpaRepository.deleteByPostIdAndUserId(postId, userId) }
    }

    test("deleteByPostIdAndIpAddress should delegate to JpaRepository and return count") {
        val postId = Tsid.generate().decode()
        val ipAddress = "127.0.0.1"

        every { postLikeJpaRepository.deleteByPostIdAndIpAddress(postId, ipAddress) } returns 1L

        val result = postLikeRepository.deleteByPostIdAndIpAddress(postId, ipAddress)

        result shouldBe 1L
        verify(exactly = 1) { postLikeJpaRepository.deleteByPostIdAndIpAddress(postId, ipAddress) }
    }
})
