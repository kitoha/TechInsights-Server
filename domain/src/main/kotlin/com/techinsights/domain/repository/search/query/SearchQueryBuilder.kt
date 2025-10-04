package com.techinsights.domain.repository.search.query

import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.core.types.dsl.NumberExpression
import com.techinsights.domain.config.search.ScoreWeights
import com.techinsights.domain.entity.company.QCompany
import com.techinsights.domain.entity.post.QPost
import com.techinsights.domain.enums.search.SearchSortType
import org.springframework.stereotype.Component

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
    return Expressions.numberTemplate(
      Double::class.java,
      buildScoreTemplate(),
      post.title,
      query,
      company.name,
      post.content,
      post.viewCount,
      post.publishedAt,
      post.isSummary
    )
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

  private fun buildScoreTemplate(): String {
    return """
            CASE 
                WHEN LOWER({0}) LIKE LOWER({1}) || '%' THEN ${scoreWeights.titleExactStart}
                WHEN LOWER({0}) LIKE '%' || LOWER({1}) || '%' THEN ${scoreWeights.titleContains}
                ELSE 0.0
            END +
            CASE WHEN LOWER({2}) LIKE '%' || LOWER({1}) || '%' 
                THEN ${scoreWeights.companyName} ELSE 0.0 
            END +
            CASE WHEN {3} LIKE '%' || {1} || '%' 
                THEN ${scoreWeights.content} ELSE 0.0 
            END +
            log({4} + 1) * ${scoreWeights.viewCountMultiplier} +
            CASE 
                WHEN {5} > NOW() - INTERVAL '7 days' THEN ${scoreWeights.recent7days}
                WHEN {5} > NOW() - INTERVAL '30 days' THEN ${scoreWeights.recent30days}
                ELSE 0.0
            END +
            CASE WHEN {6} = true THEN ${scoreWeights.summaryBonus} ELSE 0.0 END
        """.trimIndent()
  }
}
