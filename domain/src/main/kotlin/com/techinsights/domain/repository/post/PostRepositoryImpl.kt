package com.techinsights.domain.repository.post

import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import com.techinsights.domain.dto.catogory.CategorySummaryDto
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.entity.company.QCompany
import com.techinsights.domain.entity.post.QPost
import com.techinsights.domain.entity.post.QPostCategory
import com.techinsights.domain.enums.Category
import com.techinsights.domain.exception.PostNotFoundException
import com.techinsights.domain.utils.Tsid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class PostRepositoryImpl(
  private val postJpaRepository: PostJpaRepository,
  private val queryFactory: JPAQueryFactory
) : PostRepository {

  companion object {
    private const val MAX_RETRY_COUNT = 5
  }

  override fun saveAll(posts: List<PostDto>): List<PostDto> {
    val entities = posts.map { post -> post.toEntity() }
    return postJpaRepository.saveAll(entities).map { entity -> PostDto.fromEntity(entity) }
  }

  override fun findAllByUrlIn(urls: List<String>): List<PostDto> {
    val postEntity = QPost.post
    val companyEntity = QCompany.company

    val posts = queryFactory.selectFrom(postEntity)
      .leftJoin(postEntity.company, companyEntity).fetchJoin()
      .where(postEntity.url.`in`(urls))
      .fetch()

    return posts.map { post -> PostDto.fromEntity(post) }
  }

  override fun findAllByIdIn(ids: List<String>): List<PostDto> {
    val postEntity = QPost.post
    val companyEntity = QCompany.company

    val postIds = ids.map { Tsid.decode(it) }

    val posts = queryFactory.selectFrom(postEntity)
      .leftJoin(postEntity.company, companyEntity).fetchJoin()
      .where(postEntity.id.`in`(postIds))
      .fetch()

    return posts.map { post -> PostDto.fromEntity(post) }
  }


  override fun getPostById(id: String): PostDto {
    val postEntity = QPost.post
    val companyEntity = QCompany.company

    val post = queryFactory.selectFrom(postEntity)
      .leftJoin(postEntity.company, companyEntity).fetchJoin()
      .where(postEntity.id.eq(Tsid.decode(id)))
      .fetchOne()?: throw PostNotFoundException("Post with ID $id not found")

    return PostDto.fromEntity(post)
  }

  override fun existsById(id: Long): Boolean {
    val postEntity = QPost.post
    
    val count = queryFactory.selectOne()
      .from(postEntity)
      .where(postEntity.id.eq(id))
      .fetchFirst()
    
    return count != null
  }

  override fun getCompanyIdByPostId(postId: String): String {
    val postEntity = QPost.post

    val companyId = queryFactory.select(postEntity.company.id)
      .from(postEntity)
      .where(postEntity.id.eq(Tsid.decode(postId)))
      .fetchOne() ?: throw PostNotFoundException("Post with ID $postId not found")

    return Tsid.encode(companyId)
  }

  override fun findOldestNotSummarized(
    limit: Long,
    lastPublishedAt: java.time.LocalDateTime?,
    lastId: Long?
  ): List<PostDto> {
    val post = QPost.post
    val company = QCompany.company

    return queryFactory.selectFrom(post)
      .leftJoin(post.company, company).fetchJoin()
      .where(
        post.isSummary.isFalse,
        post.summaryFailureCount.lt(MAX_RETRY_COUNT),
        buildCursorCondition(post, lastPublishedAt, lastId)
      )
      .orderBy(post.publishedAt.asc(), post.id.asc())
      .limit(limit)
      .fetch()
      .map { PostDto.fromEntity(it) }
  }

  override fun findOldestSummarizedAndNotEmbedded(
    limit: Long,
    lastPublishedAt: java.time.LocalDateTime?,
    lastId: Long?
  ): List<PostDto> {
    val post = QPost.post
    val company = QCompany.company

    return queryFactory.selectFrom(post)
      .leftJoin(post.company, company).fetchJoin()
      .where(
        post.isSummary.isTrue.and(post.isEmbedding.isFalse),
        buildCursorCondition(post, lastPublishedAt, lastId)
      )
      .orderBy(post.publishedAt.asc(), post.id.asc())
      .limit(limit)
      .fetch()
      .map { PostDto.fromEntity(it) }
  }

  override fun findOldestSummarized(
    limit: Long,
    lastPublishedAt: java.time.LocalDateTime?,
    lastId: Long?
  ): List<PostDto> {
    val post = QPost.post
    val company = QCompany.company

    return queryFactory.selectFrom(post)
      .leftJoin(post.company, company).fetchJoin()
      .where(
        post.isSummary.isTrue,
        buildCursorCondition(post, lastPublishedAt, lastId)
      )
      .orderBy(post.publishedAt.asc(), post.id.asc())
      .limit(limit)
      .fetch()
      .map { PostDto.fromEntity(it) }
  }

  override fun findTopViewedPosts(limit: Long): List<PostDto> {
    val post = QPost.post
    val company = QCompany.company

    return queryFactory.selectFrom(post)
      .leftJoin(post.company, company).fetchJoin()
      .where(post.isSummary.isTrue)
      .orderBy(post.viewCount.desc())
      .limit(limit)
      .fetch()
      .map { PostDto.fromEntity(it) }
  }

  override fun getCategoryStatistics(): List<CategorySummaryDto> {
    val post = QPost.post
    val postCategory = QPostCategory.postCategory

    return queryFactory
      .select(
        Projections.constructor(
          CategorySummaryDto::class.java,
          postCategory.category,
          post.id.count(),
          post.viewCount.sum().coalesce(0L),
          post.publishedAt.max()
        )
      )
      .from(postCategory)
      .join(postCategory.post, post)
      .where(
        postCategory.category.ne(Category.All),
        post.isSummary.isTrue
      )
      .groupBy(postCategory.category)
      .fetch()
  }

  override fun getAllPosts(
    pageable: Pageable,
    companyId: String?
  ): Page<PostDto> {
    val postEntity = QPost.post
    val companyEntity = QCompany.company
    val companyIdLong = companyId?.let { Tsid.decode(it) }

    val results = queryFactory.selectFrom(postEntity)
      .leftJoin(postEntity.company, companyEntity).fetchJoin()
      .where(
        postEntity.isSummary.isTrue,
        companyIdLong?.let { postEntity.company.id.eq(it) }
      )
      .orderBy(postEntity.publishedAt.desc())
      .offset(pageable.offset)
      .limit(pageable.pageSize.toLong())
      .fetch()

    val total = queryFactory.select(postEntity.id.count())
      .from(postEntity)
      .where(
        postEntity.isSummary.isTrue,
        companyIdLong?.let { postEntity.company.id.eq(it) }
      )
      .fetchOne() ?: 0L

    val resultsDto = results.map { PostDto.fromEntity(it) }
    return PageImpl(resultsDto, pageable, total)
  }

  override fun getPostsByCategory(
    pageable: Pageable,
    category: Category,
    companyId: String?
  ): Page<PostDto> {
    val postEntity = QPost.post
    val companyEntity = QCompany.company
    val postCategory = QPostCategory.postCategory
    val companyIdLong = companyId?.let { Tsid.decode(it) }

    val results = queryFactory.selectFrom(postEntity)
      .distinct()
      .leftJoin(postEntity.company, companyEntity).fetchJoin()
      .join(postEntity.postCategories, postCategory)
      .where(
        postEntity.isSummary.isTrue,
        postCategory.category.eq(category),
        companyIdLong?.let { postEntity.company.id.eq(it) }
      )
      .orderBy(postEntity.publishedAt.desc())
      .offset(pageable.offset)
      .limit(pageable.pageSize.toLong())
      .fetch()

    val total = queryFactory.select(postEntity.id.countDistinct())
      .from(postEntity)
      .join(postEntity.postCategories, postCategory)
      .where(
        postEntity.isSummary.isTrue,
        postCategory.category.eq(category),
        companyIdLong?.let { postEntity.company.id.eq(it) }
      )
      .fetchOne() ?: 0L

    val resultsDto = results.map { PostDto.fromEntity(it) }
    return PageImpl(resultsDto, pageable, total)
  }

  @Transactional
  override fun updateEmbeddingStatusBulk(postIds: List<String>): Long {
    if (postIds.isEmpty()) {
      return 0L
    }

    val post = QPost.post
    val decodedIds = postIds.map { Tsid.decode(it) }

    return queryFactory.update(post)
      .set(post.isEmbedding, true)
      .where(post.id.`in`(decodedIds))
      .execute()
  }

  @Transactional
  override fun incrementSummaryFailureCount(postId: String) {
    val post = QPost.post
    val decodedId = Tsid.decode(postId)

    queryFactory.update(post)
      .set(post.summaryFailureCount, post.summaryFailureCount.add(1))
      .where(post.id.eq(decodedId))
      .execute()
  }

  private fun buildCursorCondition(
    post: QPost,
    lastPublishedAt: java.time.LocalDateTime?,
    lastId: Long?
  ): com.querydsl.core.types.dsl.BooleanExpression? {
    if (lastPublishedAt == null || lastId == null) {
      return null
    }

    // WHERE (published_at > lastPublishedAt) 
    //    OR (published_at = lastPublishedAt AND id > lastId)
    return post.publishedAt.gt(lastPublishedAt)
      .or(
        post.publishedAt.eq(lastPublishedAt)
          .and(post.id.gt(lastId))
      )
  }

  @Transactional
  override fun incrementLikeCount(postId: Long) {
    val post = QPost.post
    val affected = queryFactory.update(post)
      .set(post.likeCount, post.likeCount.add(1))
      .where(post.id.eq(postId))
      .execute()
      
    if (affected == 0L) {
      throw PostNotFoundException("Post with ID $postId not found")
    }
  }

  @Transactional
  override fun decrementLikeCount(postId: Long) {
    val post = QPost.post
    val affected = queryFactory.update(post)
      .set(post.likeCount, post.likeCount.subtract(1))
      .where(post.id.eq(postId).and(post.likeCount.gt(0)))
      .execute()
      
    if (affected == 0L) {
      throw PostNotFoundException("Post with ID $postId not found or like count is already 0")
    }
  }
}
