package com.techinsights.domain.service.post

import com.techinsights.domain.entity.post.PostLike
import com.techinsights.domain.repository.post.PostLikeRepository
import com.techinsights.domain.utils.Tsid
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.dao.DataIntegrityViolationException

class PostLikeSaveHelperTest : FunSpec({

    val postLikeRepository = mockk<PostLikeRepository>()
    val postLikeSaveHelper = PostLikeSaveHelper(postLikeRepository)

    beforeTest { clearAllMocks() }

    test("saveIfAbsent - 저장 성공 시 true 반환") {
        val postLike = PostLike(
            id = Tsid.generateLong(),
            postId = 1L,
            userId = 100L,
            ipAddress = "127.0.0.1"
        )
        every { postLikeRepository.saveAndFlush(postLike) } returns postLike

        val result = postLikeSaveHelper.saveIfAbsent(postLike)

        result shouldBe true
        verify(exactly = 1) { postLikeRepository.saveAndFlush(postLike) }
    }

    test("saveIfAbsent - DataIntegrityViolationException 발생 시 false 반환 (동시 요청 중복)") {
        val postLike = PostLike(
            id = Tsid.generateLong(),
            postId = 1L,
            userId = 100L,
            ipAddress = "127.0.0.1"
        )
        every { postLikeRepository.saveAndFlush(postLike) } throws DataIntegrityViolationException("Duplicate key")

        val result = postLikeSaveHelper.saveIfAbsent(postLike)

        result shouldBe false
        verify(exactly = 1) { postLikeRepository.saveAndFlush(postLike) }
    }

    test("saveIfAbsent - 익명 유저 (userId null) 저장 성공 시 true 반환") {
        val postLike = PostLike(
            id = Tsid.generateLong(),
            postId = 1L,
            userId = null,
            ipAddress = "192.168.0.1"
        )
        every { postLikeRepository.saveAndFlush(postLike) } returns postLike

        val result = postLikeSaveHelper.saveIfAbsent(postLike)

        result shouldBe true
        verify(exactly = 1) { postLikeRepository.saveAndFlush(postLike) }
    }

    test("saveIfAbsent - 익명 유저 DataIntegrityViolationException 발생 시 false 반환") {
        val postLike = PostLike(
            id = Tsid.generateLong(),
            postId = 1L,
            userId = null,
            ipAddress = "192.168.0.1"
        )
        every { postLikeRepository.saveAndFlush(postLike) } throws DataIntegrityViolationException("Duplicate key")

        val result = postLikeSaveHelper.saveIfAbsent(postLike)

        result shouldBe false
    }
})
