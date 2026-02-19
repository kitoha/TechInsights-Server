package com.techinsights.batch.summary.writer

import com.techinsights.domain.dto.company.CompanyDto
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.repository.post.PostRepository
import com.techinsights.domain.repository.post.PostSummaryFailureRepository
import com.techinsights.domain.service.company.CompanyViewCountUpdater
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.springframework.batch.item.Chunk
import java.time.LocalDateTime

class FlatteningPostWriterTest : FunSpec({

    lateinit var postRepository: PostRepository
    lateinit var companyViewCountUpdater: CompanyViewCountUpdater
    lateinit var failureRepository: PostSummaryFailureRepository
    lateinit var writer: FlatteningPostWriter

    beforeEach {
        postRepository = mockk()
        failureRepository = mockk()
        companyViewCountUpdater = mockk(relaxed = true)
        writer = FlatteningPostWriter(postRepository, failureRepository, companyViewCountUpdater)
    }

    afterEach {
        clearAllMocks()
    }

    test("write should save summarized posts and update company counts") {
        // given
        val posts = listOf(
            createPostDto("1", "Title 1", isSummary = true),
            createPostDto("2", "Title 2", isSummary = true)
        )
        val chunk = Chunk(listOf(posts))

        every { postRepository.saveAll(any<List<PostDto>>()) } returns posts

        // when
        writer.write(chunk)

        // then
        verify(exactly = 1) {
            postRepository.saveAll(match { list ->
                list.size == 2 && list.all { it.isSummary }
            })
        }
        verify(exactly = 1) {
            companyViewCountUpdater.incrementPostCount("company-1", 1)
        }
    }

    test("write should handle empty chunk") {
        // given
        val chunk = Chunk<List<PostDto>>(emptyList())

        // when
        writer.write(chunk)

        // then
        verify(exactly = 0) { postRepository.saveAll(any()) }
        verify(exactly = 0) { companyViewCountUpdater.incrementPostCount(any(), any()) }
    }

    test("write should flatten nested lists") {
        // given
        val batch1 = listOf(createPostDto("1", "Title 1", isSummary = true))
        val batch2 = listOf(
            createPostDto("2", "Title 2", isSummary = true),
            createPostDto("3", "Title 3", isSummary = true)
        )
        val chunk = Chunk(listOf(batch1, batch2))

        every { postRepository.saveAll(any<List<PostDto>>()) } returns (batch1 + batch2)

        // when
        writer.write(chunk)

        // then
        verify(exactly = 1) {
            postRepository.saveAll(match { it.size == 3 })
        }
    }

    test("write should filter out non-summarized posts") {
        // given
        val posts = listOf(
            createPostDto("1", "Title 1", isSummary = true),
            createPostDto("2", "Title 2", isSummary = false),
            createPostDto("3", "Title 3", isSummary = true)
        )
        val chunk = Chunk(listOf(posts))

        every { postRepository.saveAll(any<List<PostDto>>()) } returns listOf(posts[0], posts[2])

        // when
        writer.write(chunk)

        // then
        verify(exactly = 1) {
            postRepository.saveAll(match { it.size == 2 })
        }
    }

    test("write should handle all posts being non-summarized") {
        // given
        val posts = listOf(
            createPostDto("1", "Title 1", isSummary = false),
            createPostDto("2", "Title 2", isSummary = false)
        )
        val chunk = Chunk(listOf(posts))

        // when
        writer.write(chunk)

        // then
        verify(exactly = 0) { postRepository.saveAll(any()) }
        verify(exactly = 0) { companyViewCountUpdater.incrementPostCount(any(), any()) }
    }

    test("write should update company count for each unique company") {
        // given
        val company1 = CompanyDto("company-1", "Company 1", "https://company1.com", "")
        val company2 = CompanyDto("company-2", "Company 2", "https://company2.com", "")

        val posts = listOf(
            createPostDto("1", "Title 1", isSummary = true, company = company1),
            createPostDto("2", "Title 2", isSummary = true, company = company1),
            createPostDto("3", "Title 3", isSummary = true, company = company2)
        )
        val chunk = Chunk(listOf(posts))

        every { postRepository.saveAll(any<List<PostDto>>()) } returns posts

        // when
        writer.write(chunk)

        // then
        verify(exactly = 1) { companyViewCountUpdater.incrementPostCount("company-1", 1) }
        verify(exactly = 1) { companyViewCountUpdater.incrementPostCount("company-2", 1) }
    }

    test("write should propagate exception from postRepository") {
        // given
        val posts = listOf(createPostDto("1", "Title", isSummary = true))
        val chunk = Chunk(listOf(posts))

        every { postRepository.saveAll(any<List<PostDto>>()) } throws RuntimeException("DB Error")

        // when / then
        val exception = runCatching { writer.write(chunk) }.exceptionOrNull()
        exception.shouldBeInstanceOf<RuntimeException>()
        verify(exactly = 1) { postRepository.saveAll(any()) }
    }

    test("write should handle multiple batches with different sizes") {
        // given
        val batch1 = (1..5).map { createPostDto("$it", "Title $it", isSummary = true) }
        val batch2 = (6..8).map { createPostDto("$it", "Title $it", isSummary = true) }
        val batch3 = listOf(createPostDto("9", "Title 9", isSummary = true))
        val chunk = Chunk(listOf(batch1, batch2, batch3))

        every { postRepository.saveAll(any<List<PostDto>>()) } returns (batch1 + batch2 + batch3)

        // when
        writer.write(chunk)

        // then
        verify(exactly = 1) {
            postRepository.saveAll(match { it.size == 9 })
        }
    }

    test("write should only save and count summarized posts") {
        // given
        val posts = listOf(
            createPostDto("1", "Title 1", isSummary = true),
            createPostDto("2", "Title 2", isSummary = false),
            createPostDto("3", "Title 3", isSummary = true),
            createPostDto("4", "Title 4", isSummary = false),
            createPostDto("5", "Title 5", isSummary = true)
        )
        val chunk = Chunk(listOf(posts))

        val summarizedPosts = posts.filter { it.isSummary }
        every { postRepository.saveAll(any<List<PostDto>>()) } returns summarizedPosts

        // when
        writer.write(chunk)

        // then
        verify(exactly = 1) {
            postRepository.saveAll(match { list ->
                list.size == 3 &&
                list.map { it.id } == listOf("1", "3", "5")
            })
        }
    }

    test("write should handle posts with same company") {
        // given
        val posts = (1..10).map {
            createPostDto("$it", "Title $it", isSummary = true)
        }
        val chunk = Chunk(listOf(posts))

        every { postRepository.saveAll(any<List<PostDto>>()) } returns posts

        // when
        writer.write(chunk)

        // then
        verify(exactly = 1) {
            postRepository.saveAll(match { it.size == 10 })
        }
        verify(exactly = 1) {
            companyViewCountUpdater.incrementPostCount("company-1", 1)
        }
    }

    test("write should continue updating company counts even if one update fails") {
        // given
        val company1 = CompanyDto("company-1", "Company 1", "https://company1.com", "")
        val company2 = CompanyDto("company-2", "Company 2", "https://company2.com", "")

        val posts = listOf(
            createPostDto("1", "Title 1", isSummary = true, company = company1),
            createPostDto("2", "Title 2", isSummary = true, company = company2)
        )
        val chunk = Chunk(listOf(posts))

        every { postRepository.saveAll(any<List<PostDto>>()) } returns posts
        every { companyViewCountUpdater.incrementPostCount("company-1", 1) } throws RuntimeException("Update failed")
        every { companyViewCountUpdater.incrementPostCount("company-2", 1) } just Runs

        // when / then
        val exception = runCatching { writer.write(chunk) }.exceptionOrNull()
        exception.shouldBeInstanceOf<RuntimeException>()
    }

    test("write should handle empty nested lists") {
        // given
        val batch1 = emptyList<PostDto>()
        val batch2 = listOf(createPostDto("1", "Title", isSummary = true))
        val batch3 = emptyList<PostDto>()
        val chunk = Chunk(listOf(batch1, batch2, batch3))

        every { postRepository.saveAll(any<List<PostDto>>()) } returns batch2

        // when
        writer.write(chunk)

        // then
        verify(exactly = 1) {
            postRepository.saveAll(match { it.size == 1 })
        }
    }

    test("write should preserve post data when saving") {
        // given
        val originalPost = createPostDto(
            id = "1",
            title = "Original Title",
            isSummary = true,
            content = "Summarized content",
            preview = "Preview text"
        )
        val chunk = Chunk(listOf(listOf(originalPost)))

        val capturedPosts = slot<List<PostDto>>()
        every { postRepository.saveAll(capture(capturedPosts)) } returns listOf(originalPost)

        // when
        writer.write(chunk)

        // then
        val saved = capturedPosts.captured[0]
        saved.id shouldBe "1"
        saved.title shouldBe "Original Title"
        saved.isSummary shouldBe true
        saved.content shouldBe "Summarized content"
        saved.preview shouldBe "Preview text"
    }
})

private fun createPostDto(
    id: String,
    title: String,
    isSummary: Boolean = false,
    content: String = "Content",
    preview: String? = null,
    company: CompanyDto = CompanyDto(
        id = "company-1",
        name = "Test Company",
        blogUrl = "https://example.com/rss",
        logoImageName = ""
    )
): PostDto {
    return PostDto(
        id = id,
        title = title,
        content = content,
        url = "https://example.com/$id",
        publishedAt = LocalDateTime.now(),
        company = company,
        isSummary = isSummary,
        preview = preview,
        categories = emptySet(),
        isEmbedding = false
    )
}
