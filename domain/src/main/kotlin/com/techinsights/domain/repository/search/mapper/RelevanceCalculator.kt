package com.techinsights.domain.repository.search.mapper

import com.techinsights.domain.config.search.ScoreWeights
import com.techinsights.domain.entity.post.Post
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.ln

@Component
class RelevanceCalculator(
  private val scoreWeights: ScoreWeights
) {

  fun calculate(post: Post, query: String): Double {
    var score = 0.0
    val lowerQuery = query.lowercase()

    score += calculateTitleScore(post.title, lowerQuery)
    score += calculateCompanyScore(post.company.name, lowerQuery)
    score += calculateContentScore(post.content, lowerQuery)
    score += calculatePopularityScore(post.viewCount)
    score += calculateRecencyScore(post.publishedAt)
    score += calculateSummaryScore(post.isSummary)

    return score
  }

  private fun calculateTitleScore(title: String, query: String): Double {
    val lowerTitle = title.lowercase()
    return when {
      lowerTitle.startsWith(query) -> scoreWeights.titleExactStart
      lowerTitle.contains(query) -> scoreWeights.titleContains
      else -> 0.0
    }
  }

  private fun calculateCompanyScore(companyName: String, query: String): Double {
    return if (companyName.lowercase().contains(query)) {
      scoreWeights.companyName
    } else 0.0
  }

  private fun calculateContentScore(content: String, query: String): Double {
    return if (content.lowercase().contains(query)) {
      scoreWeights.content
    } else 0.0
  }

  private fun calculatePopularityScore(viewCount: Long): Double {
    return ln(viewCount.toDouble() + 1) * scoreWeights.viewCountMultiplier
  }

  private fun calculateRecencyScore(publishedAt: LocalDateTime): Double {
    val daysSince = ChronoUnit.DAYS.between(publishedAt, LocalDateTime.now())
    return when {
      daysSince <= 7 -> scoreWeights.recent7days
      daysSince <= 30 -> scoreWeights.recent30days
      else -> 0.0
    }
  }

  private fun calculateSummaryScore(isSummary: Boolean): Double {
    return if (isSummary) scoreWeights.summaryBonus else 0.0
  }
}
