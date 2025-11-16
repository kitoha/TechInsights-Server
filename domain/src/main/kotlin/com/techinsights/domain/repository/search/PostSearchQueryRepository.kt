package com.techinsights.domain.repository.search

import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.core.types.dsl.NumberExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import com.techinsights.domain.dto.search.PostSearchProjection
import com.techinsights.domain.entity.company.QCompany
import com.techinsights.domain.entity.post.QPost
import org.springframework.stereotype.Repository

@Repository
class PostSearchQueryRepository(
  private val jpaQueryFactory: JPAQueryFactory
) {

  private val post = QPost.post
  private val company = QCompany.company

  fun findForInstantSearch(
    query: String,
    relevanceScore: NumberExpression<Double>,
    limit: Int
  ): List<PostSearchProjection> {
    val results = jpaQueryFactory
      .select(post, relevanceScore)
      .from(post)
      .join(post.company, company).fetchJoin()
      .where(
        buildInstantSearchCondition(query),
        post.isSummary.isTrue
      )
      .orderBy(
        relevanceScore.desc(),
        post.viewCount.desc()
      )
      .limit(limit.toLong())
      .fetch()

    return results.map { tuple ->
      PostSearchProjection(
        post = tuple.get(post)!!,
        relevanceScore = tuple.get(relevanceScore)!!
      )
    }
  }

  fun findForFullSearch(
    condition: BooleanExpression,
    orderSpecifiers: Array<OrderSpecifier<*>>,
    relevanceScore: NumberExpression<Double>,
    offset: Long,
    limit: Long
  ): List<PostSearchProjection> {
    val results = jpaQueryFactory
      .select(post, relevanceScore)
      .from(post)
      .join(post.company, company).fetchJoin()
      .where(
        condition,
        post.isSummary.isTrue
      )
      .orderBy(*orderSpecifiers)
      .offset(offset)
      .limit(limit)
      .fetch()

    return results.map { tuple ->
      PostSearchProjection(
        post = tuple.get(post)!!,
        relevanceScore = tuple.get(relevanceScore)!!
      )
    }
  }

  fun countByCondition(condition: BooleanExpression): Long {
    return jpaQueryFactory
      .select(post.count())
      .from(post)
      .join(post.company, company)
      .where(
        condition,
        post.isSummary.isTrue
      )
      .fetchOne() ?: 0L
  }

  fun countMatchedPostsByCompany(companyId: Long, query: String): Long {
    return jpaQueryFactory
      .select(post.count())
      .from(post)
      .where(
        post.company.id.eq(companyId),
        post.title.containsIgnoreCase(query)
          .or(post.content.contains(query)),
        post.isSummary.isTrue
      )
      .fetchOne() ?: 0L
  }

  private fun buildInstantSearchCondition(query: String): BooleanExpression {
    return post.title.containsIgnoreCase(query)
      .or(post.content.contains(query))
      .or(company.name.containsIgnoreCase(query))
  }
}