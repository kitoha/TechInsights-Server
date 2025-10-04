package com.techinsights.domain.repository.search.mapper

import org.springframework.stereotype.Component

@Component
class TextHighlighter {

  fun highlight(text: String, query: String): String {
    if (query.isBlank()) return text

    val lowerText = text.lowercase()
    val lowerQuery = query.lowercase()
    val startIndex = lowerText.indexOf(lowerQuery)

    if (startIndex == -1) return text

    val before = text.substring(0, startIndex)
    val matched = text.substring(startIndex, startIndex + query.length)
    val after = text.substring(startIndex + query.length)

    return "$before<mark>$matched</mark>$after"
  }
}