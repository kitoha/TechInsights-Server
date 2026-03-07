package com.techinsights.domain.service.github

import com.techinsights.domain.dto.auth.Requester
import com.techinsights.domain.dto.github.GithubRepositoryDto
import com.techinsights.domain.entity.github.GithubBookmark
import com.techinsights.domain.exception.GithubRepositoryNotFoundException
import com.techinsights.domain.exception.UnauthorizedException
import com.techinsights.domain.repository.github.GithubBookmarkRepository
import com.techinsights.domain.repository.github.GithubRepositoryRepository
import com.techinsights.domain.utils.Tsid
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class GithubBookmarkServiceTest : FunSpec({

    val githubBookmarkRepository = mockk<GithubBookmarkRepository>()
    val githubRepositoryRepository = mockk<GithubRepositoryRepository>()
    val githubBookmarkSaveHelper = mockk<GithubBookmarkSaveHelper>()
    val service = GithubBookmarkService(githubBookmarkRepository, githubRepositoryRepository, githubBookmarkSaveHelper)

    val repoId = 200L
    val authenticatedUser = Requester.Authenticated(userId = 1L, ip = "127.0.0.1")
    val anonymousUser = Requester.Anonymous(anonymousId = "anon-abc", ip = "127.0.0.1")

    beforeTest { clearAllMocks() }

    test("toggleBookmark - 익명 유저 요청 시 UnauthorizedException 발생") {
        shouldThrow<UnauthorizedException> {
            service.toggleBookmark(repoId, anonymousUser)
        }
        verify(exactly = 0) { githubRepositoryRepository.findById(any()) }
    }

    test("toggleBookmark - 존재하지 않는 레포지토리면 GithubRepositoryNotFoundException 발생") {
        every { githubRepositoryRepository.findById(repoId) } returns null

        shouldThrow<GithubRepositoryNotFoundException> {
            service.toggleBookmark(repoId, authenticatedUser)
        }
    }

    test("toggleBookmark - 북마크 없을 때 saveIfAbsent 호출하고 true 반환") {
        every { githubRepositoryRepository.findById(repoId) } returns mockk<GithubRepositoryDto>(relaxed = true)
        every { githubBookmarkRepository.findByRepoIdAndUserId(repoId, 1L) } returns null
        every { githubBookmarkSaveHelper.saveIfAbsent(any()) } returns true

        val result = service.toggleBookmark(repoId, authenticatedUser)

        result shouldBe true
        verify(exactly = 1) { githubBookmarkSaveHelper.saveIfAbsent(any()) }
        verify(exactly = 0) { githubBookmarkRepository.deleteByRepoIdAndUserId(any(), any()) }
    }

    test("toggleBookmark - race condition: saveIfAbsent false 반환 시 false 반환") {
        every { githubRepositoryRepository.findById(repoId) } returns mockk<GithubRepositoryDto>(relaxed = true)
        every { githubBookmarkRepository.findByRepoIdAndUserId(repoId, 1L) } returns null
        every { githubBookmarkSaveHelper.saveIfAbsent(any()) } returns false

        val result = service.toggleBookmark(repoId, authenticatedUser)

        result shouldBe false
        verify(exactly = 0) { githubBookmarkRepository.deleteByRepoIdAndUserId(any(), any()) }
    }

    test("toggleBookmark - 이미 북마크된 경우 delete 호출하고 false 반환") {
        val existing = GithubBookmark(id = Tsid.generateLong(), repoId = repoId, userId = 1L)
        every { githubRepositoryRepository.findById(repoId) } returns mockk<GithubRepositoryDto>(relaxed = true)
        every { githubBookmarkRepository.findByRepoIdAndUserId(repoId, 1L) } returns existing
        every { githubBookmarkRepository.deleteByRepoIdAndUserId(repoId, 1L) } returns 1L

        val result = service.toggleBookmark(repoId, authenticatedUser)

        result shouldBe false
        verify(exactly = 1) { githubBookmarkRepository.deleteByRepoIdAndUserId(repoId, 1L) }
        verify(exactly = 0) { githubBookmarkSaveHelper.saveIfAbsent(any()) }
    }
})
