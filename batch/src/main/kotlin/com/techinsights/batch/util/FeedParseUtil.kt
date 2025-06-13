package com.techinsights.batch.util

import org.w3c.dom.Element as DomElement
import java.time.LocalDateTime
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object FeedParseUtil {

  fun DomElement.getSingleTagText(vararg tags: String): String {
    for (tag in tags) {
      val nodeList = this.getElementsByTagName(tag)
      if (nodeList.length > 0) {
        val node = nodeList.item(0)
        if (node is DomElement && node.hasAttribute("href")) {
          return node.getAttribute("href")
        }
        if (node.textContent.isNullOrBlank().not()) {
          return node.textContent
        }
      }
    }
    return ""
  }

  fun parseRssDate(vararg dateStrings: String): LocalDateTime {
    for (dateString in dateStrings) {
      try {
        if (dateString.isNotBlank()) {
          val formatter = DateTimeFormatter.RFC_1123_DATE_TIME
          return ZonedDateTime.parse(dateString, formatter).toLocalDateTime()
        }
      } catch (ignored: Exception) {}
    }
    return LocalDateTime.now()
  }

  fun parseHtmlDate(dateText: String?): LocalDateTime {
    return try {
      LocalDate.parse(dateText, DateTimeFormatter.ofPattern("yyyy-MM-dd")).atStartOfDay()
    } catch (e: Exception) {
      LocalDateTime.now()
    }
  }
}