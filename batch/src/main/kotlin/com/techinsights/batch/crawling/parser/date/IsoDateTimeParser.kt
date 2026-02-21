package com.techinsights.batch.crawling.parser.date

import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class IsoDateTimeParser : DateParseStrategy {

  override fun parse(dateString: String): LocalDateTime? = runCatching {
    ZonedDateTime.parse(dateString, DateTimeFormatter.ISO_DATE_TIME)
      .toLocalDateTime()
  }.getOrNull()
}