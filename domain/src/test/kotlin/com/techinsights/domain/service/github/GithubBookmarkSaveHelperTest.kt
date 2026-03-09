package com.techinsights.domain.service.github

import com.techinsights.domain.entity.github.GithubBookmark
import com.techinsights.domain.repository.github.GithubBookmarkRepository
import com.techinsights.domain.utils.Tsid
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.dao.DataIntegrityViolationException

class GithubBookmarkSaveHelperTest : FunSpec({

    val githubBookmarkRepository = mockk<GithubBookmarkRepository>()
    val helper = GithubBookmarkSaveHelper(githubBookmarkRepository)

    val bookmark = GithubBookmark(
        id = Tsid.generateLong(),
        repoId = 100L,
        userId = 1L,
    )

    beforeTest { clearAllMocks() }

    test("saveIfAbsent - 정상 저장 시 saveAndFlush 호출 후 true 반환") {
        // given
        every { githubBookmarkRepository.saveAndFlush(bookmark) } returns bookmark

        // when
        val result = helper.saveIfAbsent(bookmark)

        // then
        result shouldBe true
        verify(exactly = 1) { githubBookmarkRepository.saveAndFlush(bookmark) }
    }

    test("saveIfAbsent - DataIntegrityViolationException (race condition) 시 false 반환") {
        // given
        every { githubBookmarkRepository.saveAndFlush(bookmark) } throws DataIntegrityViolationException("duplicate key value violates unique constraint")

        // when
        val result = helper.saveIfAbsent(bookmark)

        // then
        result shouldBe false
        verify(exactly = 1) { githubBookmarkRepository.saveAndFlush(bookmark) }
    }
})
