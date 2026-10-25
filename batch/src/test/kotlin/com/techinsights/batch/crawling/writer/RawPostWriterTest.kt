package com.techinsights.batch.crawling.writer

import com.techinsights.domain.dto.company.CompanyDto
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.repository.post.PostRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.batch.item.Chunk
import java.time.LocalDateTime

class RawPostWriterTest : FunSpec({

  val postRepository = mockk<PostRepository>()
  val writer = RawPostWriter(postRepository)

  beforeEach {
    clearMocks(postRepository)
  }

  test("write should insert posts without an existence pre-check") {
    val posts = listOf(createPost("1", "https://a.com/1"), createPost("2", "https://a.com/2"))
    val captured = slot<List<PostDto>>()
    every { postRepository.insertAllIgnoringDuplicates(capture(captured)) } returns 2

    writer.write(Chunk(listOf(posts)))

    captured.captured.map { it.url } shouldContainExactly listOf("https://a.com/1", "https://a.com/2")
    verify(exactly = 0) { postRepository.findAllByUrlIn(any()) }
  }

  test("write should de-duplicate posts with the same url within a chunk") {
    val first = createPost("1", "https://a.com/same")
    val second = createPost("2", "https://a.com/same")
    val other = createPost("3", "https://a.com/other")
    val captured = slot<List<PostDto>>()
    every { postRepository.insertAllIgnoringDuplicates(capture(captured)) } returns 2

    writer.write(Chunk(listOf(listOf(first, second), listOf(other))))

    captured.captured.map { it.id } shouldContainExactly listOf("1", "3")
  }

  test("write should keep a valid duplicate when the first post with that url has blank content") {
    val blankFirst = createPost("1", "https://a.com/same", content = " ")
    val validSecond = createPost("2", "https://a.com/same")
    val captured = slot<List<PostDto>>()
    every { postRepository.insertAllIgnoringDuplicates(capture(captured)) } returns 1

    writer.write(Chunk(listOf(listOf(blankFirst, validSecond))))

    captured.captured.map { it.id } shouldContainExactly listOf("2")
  }

  test("write should skip posts with blank content") {
    val blank = createPost("1", "https://a.com/1", content = " ")
    val valid = createPost("2", "https://a.com/2")
    val captured = slot<List<PostDto>>()
    every { postRepository.insertAllIgnoringDuplicates(capture(captured)) } returns 1

    writer.write(Chunk(listOf(listOf(blank, valid))))

    captured.captured.map { it.id } shouldContainExactly listOf("2")
  }

  test("write should not touch the repository when nothing is left to insert") {
    val blank = createPost("1", "https://a.com/1", content = "")

    writer.write(Chunk(listOf(listOf(blank))))

    verify(exactly = 0) { postRepository.insertAllIgnoringDuplicates(any()) }
  }

  test("write should do nothing for an empty chunk") {
    writer.write(Chunk<List<PostDto>>(emptyList()))

    verify(exactly = 0) { postRepository.insertAllIgnoringDuplicates(any()) }
  }
})

private fun createPost(id: String, url: String, content: String = "content") = PostDto(
  id = id,
  title = "title $id",
  preview = null,
  url = url,
  content = content,
  publishedAt = LocalDateTime.of(2024, 1, 1, 0, 0),
  company = CompanyDto("company-1", "Company", "https://a.com", ""),
  categories = emptySet()
)
