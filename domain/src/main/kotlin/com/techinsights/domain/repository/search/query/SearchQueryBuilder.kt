package com.techinsights.domain.repository.search.query

import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.core.types.dsl.CaseBuilder
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.core.types.dsl.NumberExpression
import com.techinsights.domain.config.search.ScoreWeights
import com.techinsights.domain.entity.company.QCompany
import com.techinsights.domain.entity.post.QPost
import com.techinsights.domain.enums.search.SearchSortType
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class SearchQueryBuilder(
  private val scoreWeights: ScoreWeights
) {
  private val post = QPost.post
  private val company = QCompany.company

  fun buildSearchCondition(query: String, companyId: Long? = null): BooleanExpression {
    val baseCondition = post.title.containsIgnoreCase(query)
      .or(post.preview.containsIgnoreCase(query))
      .or(post.content.contains(query))
      .or(company.name.containsIgnoreCase(query))

    return companyId?.let {
      baseCondition.and(post.company.id.eq(it))
    } ?: baseCondition
  }

  fun buildRelevanceScore(query: String): NumberExpression<Double> {
    val lowerQuery = query.lowercase()
    val sevenDaysAgo = LocalDateTime.now().minusDays(7)
    val thirtyDaysAgo = LocalDateTime.now().minusDays(30)

    // 제목 점수
    val titleScore = CaseBuilder()
      .`when`(post.title.lower().startsWith(lowerQuery))
      .then(scoreWeights.titleExactStart)
      .`when`(post.title.lower().contains(lowerQuery))
      .then(scoreWeights.titleContains)
      .otherwise(0.0)

    // 회사명 점수
    val companyScore = CaseBuilder()
      .`when`(company.name.lower().contains(lowerQuery))
      .then(scoreWeights.companyName)
      .otherwise(0.0)

    // 본문 점수
    val contentScore = CaseBuilder()
      .`when`(post.content.contains(query))
      .then(scoreWeights.content)
      .otherwise(0.0)

    // 인기도 점수
    val popularityScore = Expressions.numberTemplate(
      Double::class.java,
      "log({0} + 1) * {1}",
      post.viewCount,
      scoreWeights.viewCountMultiplier
    )

    // 최신성 점수
    val recencyScore = CaseBuilder()
      .`when`(post.publishedAt.after(sevenDaysAgo))
      .then(scoreWeights.recent7days)
      .`when`(post.publishedAt.after(thirtyDaysAgo))
      .then(scoreWeights.recent30days)
      .otherwise(0.0)

    // 요약 점수
    val summaryScore = CaseBuilder()
      .`when`(post.isSummary.isTrue)
      .then(scoreWeights.summaryBonus)
      .otherwise(0.0)

    // 모든 점수 합산
    return titleScore
      .add(companyScore)
      .add(contentScore)
      .add(popularityScore)
      .add(recencyScore)
      .add(summaryScore)
  }

  fun buildOrderSpecifier(
    sortBy: SearchSortType,
    relevanceScore: NumberExpression<Double>
  ): Array<OrderSpecifier<*>> {
    return when (sortBy) {
      SearchSortType.RELEVANCE -> arrayOf(
        relevanceScore.desc(),
        post.publishedAt.desc()
      )
      SearchSortType.LATEST -> arrayOf(
        post.publishedAt.desc()
      )
      SearchSortType.POPULAR -> arrayOf(
        post.viewCount.desc(),
        post.publishedAt.desc()
      )
    }
  }

  fun buildSimilarityScore(query: String): NumberExpression<Double> {
    return Expressions.numberTemplate(
      Double::class.java,
      "similarity({0}, {1})",
      post.title,
      query
    )
  }
}