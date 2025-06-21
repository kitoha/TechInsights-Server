package com.techinsights.domain.repository.post

import com.querydsl.jpa.impl.JPAQueryFactory
import com.techinsights.domain.dto.company.CompanyDto
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.entity.company.QCompany
import com.techinsights.domain.entity.post.Post
import com.techinsights.domain.entity.post.QPost
import com.techinsights.domain.exeption.PostNotFoundException
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
    val posts: List<Post> = postJpaRepository.findAllByUrlIn(urls)
    return posts.map { post -> PostDto.fromEntity(post) }
  }



  override fun getPosts(pageable: Pageable): Page<PostDto> {
    val postEntity = QPost.post
    val companyEntity = QCompany.company

    val query = queryFactory.selectFrom(postEntity)
      .leftJoin(postEntity.company, companyEntity).fetchJoin()
      .orderBy(postEntity.publishedAt.desc())
      .offset(pageable.offset)
      .limit(pageable.pageSize.toLong())

    val results = query.fetch()
    val total = queryFactory.selectFrom(postEntity).fetchCount()

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
}