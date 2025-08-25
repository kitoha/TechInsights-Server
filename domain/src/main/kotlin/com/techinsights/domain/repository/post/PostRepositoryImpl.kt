package com.techinsights.domain.repository.post

import com.querydsl.jpa.impl.JPAQueryFactory
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.entity.company.QCompany
import com.techinsights.domain.entity.post.Post
import com.techinsights.domain.entity.post.QPost
import com.techinsights.domain.enums.Category
import com.techinsights.domain.exception.PostNotFoundException
import com.techinsights.domain.utils.Tsid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class PostRepositoryImpl(
  private val postJpaRepository: PostJpaRepository,
  private val queryFactory: JPAQueryFactory
) : PostRepository {

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

  override fun getPosts(pageable: Pageable, category: Category): Page<PostDto> {
    val postEntity = QPost.post
    val companyEntity = QCompany.company

    val query = queryFactory.selectFrom(postEntity)
      .leftJoin(postEntity.company, companyEntity).fetchJoin()
      .where(postEntity.isSummary.isTrue,
        category.takeIf { it != Category.All }
          ?.let { postEntity.categories.contains(it) })
      .orderBy(postEntity.publishedAt.desc())
      .offset(pageable.offset)
      .limit(pageable.pageSize.toLong())

    val results = query.fetch()
    val total = queryFactory.select(postEntity.id.count())
      .where(postEntity.isSummary.isTrue)
      .from(postEntity).fetchOne() ?: 0L

    val resultsDto = results.map { post -> PostDto.fromEntity(post) }

    return PageImpl(resultsDto, pageable, total)
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

  override fun findOldestNotSummarized(limit: Long, offset: Long): List<PostDto> {
    val post = QPost.post
    val company = QCompany.company

    return queryFactory.selectFrom(post)
      .leftJoin(post.company, company).fetchJoin()
      .where(post.isSummary.isFalse)
      .orderBy(post.publishedAt.asc())
      .offset(offset)
      .limit(limit)
      .fetch()
      .map { PostDto.fromEntity(it) }
  }

  override fun findOldestSummarized(limit: Long, offset: Long): List<PostDto> {
    val post = QPost.post
    val company = QCompany.company

    return queryFactory.selectFrom(post)
      .leftJoin(post.company, company).fetchJoin()
      .where(post.isSummary.isTrue)
      .orderBy(post.publishedAt.asc())
      .offset(offset)
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
}