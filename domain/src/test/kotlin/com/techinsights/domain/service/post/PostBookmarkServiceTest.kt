package com.techinsights.domain.service.post

import com.techinsights.domain.dto.auth.Requester
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.entity.post.PostBookmark
import com.techinsights.domain.exception.PostNotFoundException
import com.techinsights.domain.exception.UnauthorizedException
import com.techinsights.domain.repository.post.PostBookmarkRepository
import com.techinsights.domain.repository.post.PostRepository
import com.techinsights.domain.utils.Tsid
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

class PostBookmarkServiceTest : FunSpec({

    val postBookmarkRepository = mockk<PostBookmarkRepository>()
    val postRepository = mockk<PostRepository>()
    val postBookmarkSaveHelper = mockk<PostBookmarkSaveHelper>()
    val service = PostBookmarkService(postBookmarkRepository, postRepository, postBookmarkSaveHelper)

    val postIdStr = Tsid.generate()
    val postId = Tsid.decode(postIdStr)
    val authenticatedUser = Requester.Authenticated(userId = 1L, ip = "127.0.0.1")
    val anonymousUser = Requester.Anonymous(anonymousId = "anon-abc", ip = "127.0.0.1")

    beforeTest { clearAllMocks() }

    test("toggleBookmark - 익명 유저 요청 시 UnauthorizedException 발생") {
        shouldThrow<UnauthorizedException> {
            service.toggleBookmark(postIdStr, anonymousUser)
        }
        verify(exactly = 0) { postRepository.existsById(any()) }
    }

    test("toggleBookmark - 존재하지 않는 게시글이면 PostNotFoundException 발생") {
        every { postRepository.existsById(postId) } returns false

        shouldThrow<PostNotFoundException> {
            service.toggleBookmark(postIdStr, authenticatedUser)
        }
    }

    test("toggleBookmark - 북마크 없을 때 saveIfAbsent 호출하고 true 반환") {
        every { postRepository.existsById(postId) } returns true
        every { postBookmarkRepository.findByPostIdAndUserId(postId, 1L) } returns null
        every { postBookmarkSaveHelper.saveIfAbsent(any()) } returns true

        val result = service.toggleBookmark(postIdStr, authenticatedUser)

        result shouldBe true
        verify(exactly = 1) { postBookmarkSaveHelper.saveIfAbsent(any()) }
        verify(exactly = 0) { postBookmarkRepository.deleteByPostIdAndUserId(any(), any()) }
    }

    test("toggleBookmark - race condition: saveIfAbsent false 반환 시 false 반환") {
        every { postRepository.existsById(postId) } returns true
        every { postBookmarkRepository.findByPostIdAndUserId(postId, 1L) } returns null
        every { postBookmarkSaveHelper.saveIfAbsent(any()) } returns false

        val result = service.toggleBookmark(postIdStr, authenticatedUser)

        result shouldBe false
        verify(exactly = 0) { postBookmarkRepository.deleteByPostIdAndUserId(any(), any()) }
    }

    test("toggleBookmark - 이미 북마크된 경우 delete 호출하고 false 반환") {
        val existing = PostBookmark(id = Tsid.generateLong(), postId = postId, userId = 1L)
        every { postRepository.existsById(postId) } returns true
        every { postBookmarkRepository.findByPostIdAndUserId(postId, 1L) } returns existing
        every { postBookmarkRepository.deleteByPostIdAndUserId(postId, 1L) } returns 1L

        val result = service.toggleBookmark(postIdStr, authenticatedUser)

        result shouldBe false
        verify(exactly = 1) { postBookmarkRepository.deleteByPostIdAndUserId(postId, 1L) }
        verify(exactly = 0) { postBookmarkSaveHelper.saveIfAbsent(any()) }
    }

    test("getMyBookmarks - findBookmarkedPosts에 위임하고 결과 반환") {
        val pageable = PageRequest.of(0, 20)
        val mockPage = PageImpl<PostDto>(emptyList(), pageable, 0)
        every { postBookmarkRepository.findBookmarkedPosts(1L, pageable) } returns mockPage

        val result = service.getMyBookmarks(1L, pageable)

        result shouldBe mockPage
        verify(exactly = 1) { postBookmarkRepository.findBookmarkedPosts(1L, pageable) }
    }

    test("countMyBookmarks - countByUserId에 위임하고 결과 반환") {
        every { postBookmarkRepository.countByUserId(1L) } returns 7L

        val result = service.countMyBookmarks(1L)

        result shouldBe 7L
        verify(exactly = 1) { postBookmarkRepository.countByUserId(1L) }
    }
})
