package com.techinsights.domain.repository.post

import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQuery
import com.querydsl.jpa.impl.JPAQueryFactory
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.entity.company.Company
import com.techinsights.domain.entity.company.QCompany
import com.techinsights.domain.entity.post.Post
import com.techinsights.domain.entity.post.QPost
import com.techinsights.domain.enums.Category
import com.techinsights.domain.exception.PostNotFoundException
import com.techinsights.domain.utils.Tsid
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime

class PostRepositoryImplTest : FunSpec({

  val postJpaRepository = mockk<PostJpaRepository>()
  val queryFactory = mockk<JPAQueryFactory>()
  val repository = PostRepositoryImpl(postJpaRepository, queryFactory)

  val company = Company(
    id = 1L,
    name = "Test Company",
    blogUrl = "http://testcompany.com/blog",
    logoImageName = "logo.png",
    rssSupported = true
  )

  val post1 = Post(
    id = 1L,
    title = "Test Post 1",
    url = "https://test.com/post1",
    content = "Content 1",
    publishedAt = LocalDateTime.of(2024, 1, 1, 0, 0),
    company = company,
    isSummary = true,
    isEmbedding = true,
    viewCount = 100,
    categories = mutableSetOf(Category.AI, Category.BackEnd),
    preview = "Preview 1",
    thumbnail = "thumbnail1.png"
  )

  val post2 = Post(
    id = 2L,
    title = "Test Post 2",
    url = "https://test.com/post2",
    content = "Content 2",
    publishedAt = LocalDateTime.of(2024, 1, 2, 0, 0),
    company = company,
    isSummary = false,
    isEmbedding = false,
    viewCount = 50,
    categories = mutableSetOf(Category.FrontEnd),
    preview = "Preview 1",
    thumbnail = "thumbnail1.png"
  )

  beforeTest {
    clearAllMocks()
  }

  test("should save all posts and return PostDto list") {
    val postDtos = listOf(PostDto.fromEntity(post1), PostDto.fromEntity(post2))
    val entities = postDtos.map { it.toEntity() }

    every { postJpaRepository.saveAll(any<List<Post>>()) } returns entities

    val result = repository.saveAll(postDtos)

    result shouldHaveSize 2
    result[0].title shouldBe "Test Post 1"
    result[1].title shouldBe "Test Post 2"
    verify(exactly = 1) { postJpaRepository.saveAll(any<List<Post>>()) }
  }

  test("should return empty list when input is empty") {
    every { postJpaRepository.saveAll(emptyList()) } returns emptyList()

    val result = repository.saveAll(emptyList())

    result.shouldBeEmpty()
    verify(exactly = 1) { postJpaRepository.saveAll(emptyList()) }
  }

  test("should find posts by urls with fetchJoin") {
    val urls = listOf("https://test.com/post1", "https://test.com/post2")
    val query = mockk<JPAQuery<Post>>()

    every { queryFactory.selectFrom(QPost.post) } returns query
    every { query.leftJoin(QPost.post.company, QCompany.company) } returns query
    every { query.fetchJoin() } returns query
    every { query.where(any<BooleanExpression>()) } returns query
    every { query.fetch() } returns listOf(post1, post2)

    val result = repository.findAllByUrlIn(urls)

    result shouldHaveSize 2
    result[0].url shouldBe post1.url
    result[1].url shouldBe post2.url
    verify(exactly = 1) { query.fetchJoin() }
  }

  test("should return empty list when no posts found") {
    val query = mockk<JPAQuery<Post>>()

    every { queryFactory.selectFrom(QPost.post) } returns query
    every { query.leftJoin(QPost.post.company, QCompany.company) } returns query
    every { query.fetchJoin() } returns query
    every { query.where(any<BooleanExpression>()) } returns query
    every { query.fetch() } returns emptyList()

    val result = repository.findAllByUrlIn(listOf("non-existent"))

    result.shouldBeEmpty()
  }

  test("should find posts by TSID encoded ids") {
    val encodedIds = listOf(Tsid.encode(1L), Tsid.encode(2L))
    val query = mockk<JPAQuery<Post>>()

    every { queryFactory.selectFrom(QPost.post) } returns query
    every { query.leftJoin(QPost.post.company, QCompany.company) } returns query
    every { query.fetchJoin() } returns query
    every { query.where(any<BooleanExpression>()) } returns query
    every { query.fetch() } returns listOf(post1, post2)

    val result = repository.findAllByIdIn(encodedIds)

    result shouldHaveSize 2
    verify(exactly = 1) { query.fetchJoin() }
  }

  test("should return post when found") {
    val encodedId = Tsid.encode(1L)
    val query = mockk<JPAQuery<Post>>()

    every { queryFactory.selectFrom(QPost.post) } returns query
    every { query.leftJoin(QPost.post.company, QCompany.company) } returns query
    every { query.fetchJoin() } returns query
    every { query.where(any<BooleanExpression>()) } returns query
    every { query.fetchOne() } returns post1

    val result = repository.getPostById(encodedId)

    result.id shouldBe Tsid.encode(post1.id)
    result.title shouldBe post1.title
  }

  test("should throw PostNotFoundException when post not found") {
    val encodedId = Tsid.encode(999L)
    val query = mockk<JPAQuery<Post>>()

    every { queryFactory.selectFrom(QPost.post) } returns query
    every { query.leftJoin(QPost.post.company, QCompany.company) } returns query
    every { query.fetchJoin() } returns query
    every { query.where(any<BooleanExpression>()) } returns query
    every { query.fetchOne() } returns null

    shouldThrow<PostNotFoundException> {
      repository.getPostById(encodedId)
    }
  }

  test("should return oldest posts without summary ordered by publishedAt") {
    val query = mockk<JPAQuery<Post>>()

    every { queryFactory.selectFrom(QPost.post) } returns query
    every { query.leftJoin(QPost.post.company, QCompany.company) } returns query
    every { query.fetchJoin() } returns query
    every { query.where(any<BooleanExpression>()) } returns query
    every { query.orderBy(any()) } returns query
    every { query.offset(0L) } returns query
    every { query.limit(10L) } returns query
    every { query.fetch() } returns listOf(post2)

    val result = repository.findOldestNotSummarized(limit = 10, lastPublishedAt = null, lastId = null)

    result shouldHaveSize 1
    result[0].isSummary shouldBe false
    verify(exactly = 1) { query.orderBy(any()) }
  }

  test("should apply cursor and limit correctly") {
    val query = mockk<JPAQuery<Post>>()
    val cursorDate = LocalDateTime.of(2024, 1, 1, 12, 0)
    val cursorId = 100L

    every { queryFactory.selectFrom(QPost.post) } returns query
    every { query.leftJoin(QPost.post.company, QCompany.company) } returns query
    every { query.fetchJoin() } returns query
    every { query.where(any<BooleanExpression>(), any<BooleanExpression>()) } returns query
    every { query.orderBy(any(), any()) } returns query
    every { query.limit(5L) } returns query
    every { query.fetch() } returns emptyList()

    repository.findOldestNotSummarized(limit = 5, lastPublishedAt = cursorDate, lastId = cursorId)

    verify(exactly = 1) { query.limit(5L) }
    verify(exactly = 0) { query.offset(any()) }
  }

  test("should return posts that are summarized but not embedded") {
    val postNotEmbedded = Post(
      id = 1L,
      title = "Test Post 1",
      url = "https://test.com/post1",
      content = "Content 1",
      publishedAt = LocalDateTime.of(2024, 1, 1, 0, 0),
      company = company,
      isSummary = true,
      isEmbedding = false,
      viewCount = 100,
      categories = mutableSetOf(Category.AI, Category.BackEnd),
      preview = null
    )
    val query = mockk<JPAQuery<Post>>()

    every { queryFactory.selectFrom(QPost.post) } returns query
    every { query.leftJoin(QPost.post.company, QCompany.company) } returns query
    every { query.fetchJoin() } returns query
    every { query.where(any<BooleanExpression>()) } returns query
    every { query.orderBy(any()) } returns query
    every { query.offset(0L) } returns query
    every { query.limit(10L) } returns query
    every { query.fetch() } returns listOf(postNotEmbedded)

    val result = repository.findOldestSummarizedAndNotEmbedded(limit = 10, lastPublishedAt = null, lastId = null)

    result shouldHaveSize 1
    result[0].isSummary shouldBe true
    result[0].isEmbedding shouldBe false
  }

  test("should return all summarized posts") {
    val query = mockk<JPAQuery<Post>>()

    every { queryFactory.selectFrom(QPost.post) } returns query
    every { query.leftJoin(QPost.post.company, QCompany.company) } returns query
    every { query.fetchJoin() } returns query
    every { query.where(any<BooleanExpression>()) } returns query
    every { query.orderBy(any()) } returns query
    every { query.offset(0L) } returns query
    every { query.limit(10L) } returns query
    every { query.fetch() } returns listOf(post1)

    val result = repository.findOldestSummarized(limit = 10, lastPublishedAt = null, lastId = null)

    result shouldHaveSize 1
    result[0].isSummary shouldBe true
  }

  test("should return posts ordered by view count descending") {
    val highViewPost = Post(
      id = 1L,
      title = "Test Post 1",
      url = "https://test.com/post1",
      content = "Content 1",
      publishedAt = LocalDateTime.of(2024, 1, 1, 0, 0),
      company = company,
      isSummary = true,
      isEmbedding = true,
      viewCount = 200,
      categories = mutableSetOf(Category.AI, Category.BackEnd),
      preview = null
    )
    val lowViewPost = Post(
      id = 2L,
      title = "Test Post 2",
      url = "https://test.com/post2",
      content = "Content 2",
      publishedAt = LocalDateTime.of(2024, 1, 2, 0, 0),
      company = company,
      isSummary = true,
      isEmbedding = false,
      viewCount = 50,
      categories = mutableSetOf(Category.FrontEnd),
      preview = null
    )
    val query = mockk<JPAQuery<Post>>()

    every { queryFactory.selectFrom(QPost.post) } returns query
    every { query.leftJoin(QPost.post.company, QCompany.company) } returns query
    every { query.fetchJoin() } returns query
    every { query.where(any<BooleanExpression>()) } returns query
    every { query.orderBy(any()) } returns query
    every { query.limit(10L) } returns query
    every { query.fetch() } returns listOf(highViewPost, lowViewPost)

    val result = repository.findTopViewedPosts(limit = 10)

    result shouldHaveSize 2
    result[0].viewCount shouldBe 200
    verify(exactly = 1) { query.orderBy(any()) }
  }

  test("should calculate category statistics correctly") {
    val query = mockk<JPAQuery<Post>>()

    every { queryFactory.selectFrom(QPost.post) } returns query
    every { query.fetch() } returns listOf(post1, post2)

    val result = repository.getCategoryStatistics()

    result shouldHaveSize 3
    result.find { it.category == Category.AI }?.let {
      it.postCount shouldBe 1
      it.totalViewCount shouldBe 100
    }
  }

  test("should exclude Category.All from statistics") {
    val postWithAll = Post(
      id = 1L,
      title = "Test Post 1",
      url = "https://test.com/post1",
      content = "Content 1",
      publishedAt = LocalDateTime.of(2024, 1, 1, 0, 0),
      company = company,
      isSummary = true,
      isEmbedding = true,
      viewCount = 100,
      categories = mutableSetOf(Category.All, Category.AI),
      preview = null
    )
    val query = mockk<JPAQuery<Post>>()

    every { queryFactory.selectFrom(QPost.post) } returns query
    every { query.fetch() } returns listOf(postWithAll)

    val result = repository.getCategoryStatistics()

    result.none { it.category == Category.All } shouldBe true
  }

  test("should return paginated summarized posts") {
    val pageable = PageRequest.of(0, 10)
    val query = mockk<JPAQuery<Post>>()
    val countQuery = mockk<JPAQuery<Long>>()

    every { queryFactory.selectFrom(QPost.post) } returns query
    every { queryFactory.select(QPost.post.id.count()) } returns countQuery
    every { query.leftJoin(QPost.post.company, QCompany.company) } returns query
    every { query.fetchJoin() } returns query
    every { query.where(any<BooleanExpression>(), isNull()) } returns query
    every { query.orderBy(any()) } returns query
    every { query.offset(0L) } returns query
    every { query.limit(10L) } returns query
    every { query.fetch() } returns listOf(post1)

    every { countQuery.from(QPost.post) } returns countQuery
    every { countQuery.where(any<BooleanExpression>(), isNull()) } returns countQuery
    every { countQuery.fetchOne() } returns 1L

    val result = repository.getAllPosts(pageable, null)

    result.content shouldHaveSize 1
    result.totalElements shouldBe 1L
    result.content[0].isSummary shouldBe true
  }

  test("should filter by companyId when provided") {
    val companyId = Tsid.encode(1L)
    val pageable = PageRequest.of(0, 10)
    val query = mockk<JPAQuery<Post>>()
    val countQuery = mockk<JPAQuery<Long>>()

    every { queryFactory.selectFrom(QPost.post) } returns query
    every { queryFactory.select(QPost.post.id.count()) } returns countQuery
    every { query.leftJoin(QPost.post.company, QCompany.company) } returns query
    every { query.fetchJoin() } returns query
    every { query.where(any<BooleanExpression>(), any<BooleanExpression>()) } returns query
    every { query.orderBy(any()) } returns query
    every { query.offset(0L) } returns query
    every { query.limit(10L) } returns query
    every { query.fetch() } returns listOf(post1)

    every { countQuery.from(QPost.post) } returns countQuery
    every {
      countQuery.where(
        any<BooleanExpression>(),
        any<BooleanExpression>()
      )
    } returns countQuery
    every { countQuery.fetchOne() } returns 1L

    val result = repository.getAllPosts(pageable, companyId)
    result.content shouldHaveSize 1
  }

  test("should return posts filtered by category") {
    val category = Category.AI
    val pageable = PageRequest.of(0, 10)
    val query = mockk<JPAQuery<Post>>()
    val countQuery = mockk<JPAQuery<Long>>()

    every { queryFactory.selectFrom(QPost.post) } returns query
    every { queryFactory.select(QPost.post.id.countDistinct()) } returns countQuery
    every { query.distinct() } returns query
    every { query.leftJoin(QPost.post.company, QCompany.company) } returns query
    every { query.fetchJoin() } returns query
    every {
      query.where(
        any<BooleanExpression>(),
        any<BooleanExpression>(),
        isNull()
      )
    } returns query
    every { query.orderBy(any()) } returns query
    every { query.offset(0L) } returns query
    every { query.limit(10L) } returns query
    every { query.fetch() } returns listOf(post1)

    every { countQuery.from(QPost.post) } returns countQuery
    every {
      countQuery.where(
        any<BooleanExpression>(),
        any<BooleanExpression>(),
        isNull()
      )
    } returns countQuery
    every { countQuery.fetchOne() } returns 1L

    val result = repository.getPostsByCategory(pageable, category, null)

    result.content shouldHaveSize 1
    result.content[0].categories.contains(category) shouldBe true
    verify(exactly = 1) { query.distinct() }
  }

  test("should apply both category and company filter") {
    val category = Category.AI
    val companyId = Tsid.encode(1L)
    val pageable = PageRequest.of(0, 10)
    val query = mockk<JPAQuery<Post>>()
    val countQuery = mockk<JPAQuery<Long>>()

    every { queryFactory.selectFrom(QPost.post) } returns query
    every { queryFactory.select(QPost.post.id.countDistinct()) } returns countQuery
    every { query.distinct() } returns query
    every { query.leftJoin(QPost.post.company, QCompany.company) } returns query
    every { query.fetchJoin() } returns query
    every {
      query.where(
        any<BooleanExpression>(),
        any<BooleanExpression>(),
        any<BooleanExpression>()
      )
    } returns query
    every { query.orderBy(any()) } returns query
    every { query.offset(0L) } returns query
    every { query.limit(10L) } returns query
    every { query.fetch() } returns listOf(post1)

    every { countQuery.from(QPost.post) } returns countQuery
    every {
      countQuery.where(
        any<BooleanExpression>(),
        any<BooleanExpression>(),
        any<BooleanExpression>()
      )
    } returns countQuery
    every { countQuery.fetchOne() } returns 1L

    val result = repository.getPostsByCategory(pageable, category, companyId)

    result.content shouldHaveSize 1
  }

  test("should use countDistinct for pagination") {
    val category = Category.AI
    val pageable = PageRequest.of(0, 5)
    val query = mockk<JPAQuery<Post>>()
    val countQuery = mockk<JPAQuery<Long>>()

    every { queryFactory.selectFrom(QPost.post) } returns query
    every { queryFactory.select(QPost.post.id.countDistinct()) } returns countQuery
    every { query.distinct() } returns query
    every { query.leftJoin(QPost.post.company, QCompany.company) } returns query
    every { query.fetchJoin() } returns query
    every {
      query.where(
        any<BooleanExpression>(),
        any<BooleanExpression>(),
        isNull()
      )
    } returns query
    every { query.orderBy(any()) } returns query
    every { query.offset(0L) } returns query
    every { query.limit(5L) } returns query
    every { query.fetch() } returns listOf(post1)

    every { countQuery.from(QPost.post) } returns countQuery
    every {
      countQuery.where(
        any<BooleanExpression>(),
        any<BooleanExpression>(),
        isNull()
      )
    } returns countQuery
    every { countQuery.fetchOne() } returns 5L

    val result = repository.getPostsByCategory(pageable, category, null)

    result.totalElements shouldBe 5L
    verify(exactly = 1) { queryFactory.select(QPost.post.id.countDistinct()) }
  }

  test("getCompanyIdByPostId - 성공") {
    val postId = Tsid.encode(1L)
    val companyId = 1L
    val query = mockk<JPAQuery<Long>>()

    every { queryFactory.select(QPost.post.company.id) } returns query
    every { query.from(QPost.post) } returns query
    every { query.where(QPost.post.id.eq(Tsid.decode(postId))) } returns query
    every { query.fetchOne() } returns companyId

    val result = repository.getCompanyIdByPostId(postId)

    result shouldBe Tsid.encode(companyId)
    verify(exactly = 1) { queryFactory.select(QPost.post.company.id) }
  }

  test("getCompanyIdByPostId - 존재하지 않는 게시물") {
    val invalidPostId = Tsid.encode(999L)
    val query = mockk<JPAQuery<Long>>()

    every { queryFactory.select(QPost.post.company.id) } returns query
    every { query.from(QPost.post) } returns query
    every { query.where(QPost.post.id.eq(Tsid.decode(invalidPostId))) } returns query
    every { query.fetchOne() } returns null

    shouldThrow<PostNotFoundException> {
      repository.getCompanyIdByPostId(invalidPostId)
    }

    verify(exactly = 1) { queryFactory.select(QPost.post.company.id) }
  }

  test("updateEmbeddingStatusBulk should update all posts") {
    val postIds = listOf(Tsid.encode(1L), Tsid.encode(2L))
    val updateClause = mockk<com.querydsl.jpa.impl.JPAUpdateClause>()

    every { queryFactory.update(QPost.post) } returns updateClause
    every { updateClause.set(QPost.post.isEmbedding, true) } returns updateClause
    every { updateClause.where(any<BooleanExpression>()) } returns updateClause
    every { updateClause.execute() } returns 2L

    val result = repository.updateEmbeddingStatusBulk(postIds)

    result shouldBe 2L
    verify(exactly = 1) { updateClause.execute() }
  }

  test("updateEmbeddingStatusBulk should return 0 for empty list") {
    val result = repository.updateEmbeddingStatusBulk(emptyList())

    result shouldBe 0L
    verify(exactly = 0) { queryFactory.update(any()) }
  }

  test("updateEmbeddingStatusBulk should handle single post") {
    val postIds = listOf(Tsid.encode(1L))
    val updateClause = mockk<com.querydsl.jpa.impl.JPAUpdateClause>()

    every { queryFactory.update(QPost.post) } returns updateClause
    every { updateClause.set(QPost.post.isEmbedding, true) } returns updateClause
    every { updateClause.where(any<BooleanExpression>()) } returns updateClause
    every { updateClause.execute() } returns 1L

    val result = repository.updateEmbeddingStatusBulk(postIds)

    result shouldBe 1L
  }

  test("updateEmbeddingStatusBulk should handle large batch") {
    val postIds = (1L..100L).map { Tsid.encode(it) }
    val updateClause = mockk<com.querydsl.jpa.impl.JPAUpdateClause>()

    every { queryFactory.update(QPost.post) } returns updateClause
    every { updateClause.set(QPost.post.isEmbedding, true) } returns updateClause
    every { updateClause.where(any<BooleanExpression>()) } returns updateClause
    every { updateClause.execute() } returns 100L

    val result = repository.updateEmbeddingStatusBulk(postIds)

    result shouldBe 100L
    verify(exactly = 1) { updateClause.execute() }
  }
})