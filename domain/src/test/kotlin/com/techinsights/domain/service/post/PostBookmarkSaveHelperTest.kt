package com.techinsights.domain.service.post

import com.techinsights.domain.entity.post.PostBookmark
import com.techinsights.domain.repository.post.PostBookmarkRepository
import com.techinsights.domain.utils.Tsid
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.dao.DataIntegrityViolationException

class PostBookmarkSaveHelperTest : FunSpec({

    val postBookmarkRepository = mockk<PostBookmarkRepository>()
    val helper = PostBookmarkSaveHelper(postBookmarkRepository)

    val bookmark = PostBookmark(
        id = Tsid.generateLong(),
        postId = Tsid.generateLong(),
        userId = 1L,
    )

    beforeTest { clearAllMocks() }

    test("saveIfAbsent - 정상 저장 시 saveAndFlush 호출 후 true 반환") {
        // given
        every { postBookmarkRepository.saveAndFlush(bookmark) } returns bookmark

        // when
        val result = helper.saveIfAbsent(bookmark)

        // then
        result shouldBe true
        verify(exactly = 1) { postBookmarkRepository.saveAndFlush(bookmark) }
    }

    test("saveIfAbsent - DataIntegrityViolationException (race condition) 시 false 반환") {
        // given
        every { postBookmarkRepository.saveAndFlush(bookmark) } throws DataIntegrityViolationException("duplicate key value violates unique constraint")

        // when
        val result = helper.saveIfAbsent(bookmark)

        // then
        result shouldBe false
        verify(exactly = 1) { postBookmarkRepository.saveAndFlush(bookmark) }
    }
})
